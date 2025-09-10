package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.infrastructure.persistence.product.ProductSummaryJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/listing_product_sample.sql")
@Import(ProductSummaryPersistenceAdapter::class)
@ActiveProfiles("test")
class ProductSummaryPersistenceAdapterTest
    @Autowired
    constructor(
        private val adapter: ProductSummaryPersistenceAdapter,
        private val repository: ProductSummaryJpaRepository,
    ) {
        companion object {
            @Container
            val mysql =
                MySQLContainer<Nothing>("mysql:8.0").apply {
                    withDatabaseName("grindelwald")
                    withUsername("test")
                    withPassword("test")
                    start() // 컨테이너 명시적 시작
                }

            @JvmStatic
            @DynamicPropertySource
            fun overrideProps(registry: DynamicPropertyRegistry) {
                // 컨테이너가 실행 중인지 확인
                if (!mysql.isRunning) {
                    throw IllegalStateException("MySQL 컨테이너가 실행되지 않았습니다.")
                }
                registry.add("spring.datasource.url") { mysql.jdbcUrl }
                registry.add("spring.datasource.username") { mysql.username }
                registry.add("spring.datasource.password") { mysql.password }
                registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            }
        }

        @Test
        @DisplayName("listingBy는 페이지와 사이즈에 따라 올바른 데이터를 반환한다")
        fun listingBy_withPagination() {
            // given: 페이지와 사이즈 설정
            val page1 = 0
            val size1 = 5
            val page2 = 1
            val size2 = 5

            // when
            val resultPage1 = adapter.listingBy(page1, size1)
            val resultPage2 = adapter.listingBy(page2, size2)

            // then
            assertThat(resultPage1).hasSize(5)
            assertThat(resultPage1.map { it.id }).containsExactly(1L, 2L, 3L, 4L, 5L)

            assertThat(resultPage2).hasSize(5)
            assertThat(resultPage2.map { it.id }).containsExactly(6L, 7L, 8L, 9L, 10L)
        }
    }
