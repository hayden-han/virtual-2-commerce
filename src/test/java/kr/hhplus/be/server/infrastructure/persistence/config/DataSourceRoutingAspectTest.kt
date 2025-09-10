package kr.hhplus.be.server.infrastructure.persistence.config

import jakarta.annotation.Resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@SpringBootTest
@ExtendWith(SpringExtension::class)
@Import(DataSourceRoutingAspectTest.TestService::class)
class DataSourceRoutingAspectTest
    @Autowired
    constructor(
        private val testService: TestService,
        @Autowired @Resource(name = "masterDataSource") private val masterDataSource: DataSource,
        @Autowired @Resource(name = "slaveDataSource") private val slaveDataSource: DataSource,
    ) {
        @AfterEach
        fun clearContext() {
            DataSourceContextHolder.clear()
        }

        @Test
        @DisplayName("readOnly true면 slave, false면 master에 커넥션이 맺어진다")
        fun datasourceRoutingTest() {
            val readOnlyDs = testService.getCurrentDataSourceReadOnly()
            val writeDs = testService.getCurrentDataSourceWrite()
            assertThat(readOnlyDs).isEqualTo(slaveDataSource)
            assertThat(writeDs).isEqualTo(masterDataSource)
        }

        @Service
        class TestService {
            @Transactional(readOnly = true)
            fun getCurrentDataSourceReadOnly(): DataSource =
                when (DataSourceContextHolder.get()) {
                    DataSourceType.SLAVE -> RoutingDataSourceTestUtils.getCurrentDataSource()
                    else -> throw IllegalStateException("Expected SLAVE datasource")
                }

            @Transactional(readOnly = false)
            fun getCurrentDataSourceWrite(): DataSource =
                when (DataSourceContextHolder.get()) {
                    DataSourceType.MASTER -> RoutingDataSourceTestUtils.getCurrentDataSource()
                    else -> throw IllegalStateException("Expected MASTER datasource")
                }
        }
    }

object RoutingDataSourceTestUtils {
    fun getCurrentDataSource(): DataSource =
        when (DataSourceContextHolder.get()) {
            DataSourceType.MASTER -> ApplicationContextProvider.getBean("masterDataSource", DataSource::class.java)
            DataSourceType.SLAVE -> ApplicationContextProvider.getBean("slaveDataSource", DataSource::class.java)
        }
}

@Component
object ApplicationContextProvider : ApplicationContextAware {
    private lateinit var context: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }

    fun <T> getBean(
        name: String,
        clazz: Class<T>,
    ): T = context.getBean(name, clazz)
}
