package kr.hhplus.be.server.common.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import java.io.File
import java.time.Duration

@Profile("test")
@Configuration("TestDockerComposeContainer")
class TestDockerComposeContainer {
    companion object {
        private const val MYSQL_MASTER_SERVICE_NAME = "mysql-test-master"
        private const val MYSQL_SLAVE_SERVICE_NAME = "mysql-test-slave"
        private const val MYSQL_PORT = 3306
        private const val MYSQL_DATABASE_NAME = "grindelwald"
        const val MYSQL_USERNAME = "test"
        const val MYSQL_PASSWORD = "test"

        private val logger = KotlinLogging.logger { }

        @JvmStatic
        @Container
        val mysqlContainer: DockerComposeContainer<*> =
            DockerComposeContainer(
                File("docker-compose-for-integration-test.yml"),
            ).apply {
                withExposedService(
                    MYSQL_MASTER_SERVICE_NAME,
                    MYSQL_PORT,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)),
                )
                withExposedService(
                    MYSQL_SLAVE_SERVICE_NAME,
                    MYSQL_PORT,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)),
                )
                start()
            }

        fun getMasterJdbcUrl(): String {
            val masterMappedPort =
                mysqlContainer.getServicePort(MYSQL_MASTER_SERVICE_NAME, MYSQL_PORT)
            return String.format(
                "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
                masterMappedPort,
            )
        }

        fun getSlaveJdbcUrl(): String {
            val salveMappedPort =
                mysqlContainer.getServicePort(MYSQL_SLAVE_SERVICE_NAME, MYSQL_PORT)
            return String.format(
                "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
                salveMappedPort,
            )
        }
//        @DynamicPropertySource
//        fun setProperties(registry: DynamicPropertyRegistry) {
//            val masterMappedPort =
//                mysqlContainer.getServicePort(MYSQL_MASTER_SERVICE_NAME, MYSQL_PORT)
//            val salveMappedPort =
//                mysqlContainer.getServicePort(MYSQL_MASTER_SERVICE_NAME, MYSQL_PORT)
//
//            logger.info { "MySql Master URL: ${mysqlContainer.getServiceHost(MYSQL_MASTER_SERVICE_NAME, MYSQL_PORT)}" }
//            logger.info { "MySQL Master mapped port: $masterMappedPort" }
//            logger.info { "MySql Slave URL: ${mysqlContainer.getServiceHost(MYSQL_SLAVE_SERVICE_NAME, MYSQL_PORT)}" }
//            logger.info { "MySQL Slave mapped port: $salveMappedPort" }
//
//            registry.apply {
//                add("spring.datasource.master.url") {
//                    String.format(
//                        "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
//                        masterMappedPort,
//                    )
//                }
//                add("spring.datasource.master.username") { MYSQL_USERNAME }
//                add("spring.datasource.master.password") { MYSQL_PASSWORD }
//
//                add("spring.datasource.slave.url") {
//                    String.format(
//                        "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
//                        salveMappedPort,
//                    )
//                }
//                add("spring.datasource.slave.username") { MYSQL_USERNAME }
//                add("spring.datasource.slave.password") { MYSQL_PASSWORD }
//            }
//        }
    }
}

// package kr.hhplus.be.server.common.config
//
// import io.github.oshai.kotlinlogging.KotlinLogging
// import org.springframework.test.context.DynamicPropertyRegistry
// import org.springframework.test.context.DynamicPropertySource
// import org.testcontainers.containers.DockerComposeContainer
// import org.testcontainers.junit.jupiter.Container
// import org.testcontainers.junit.jupiter.Testcontainers
// import java.io.File
//
// @Testcontainers
// open class TestDockerComposeContainer {
//    companion object {
//        private const val MYSQL_MASTER_SERVICE_NAME = "mysql-test-master"
//        private const val MYSQL_MASTER_SERVICE_PORT = 3306
//        private const val MYSQL_SLAVE_SERVICE_NAME = "mysql-test-slave"
//        private const val MYSQL_SLAVE_SERVICE_PORT = 3306
//        private const val MYSQL_DATABASE_NAME = "grindelwald"
//        private const val MYSQL_USERNAME = "test"
//        private const val MYSQL_PASSWORD = "test"
//
//        private val logger = KotlinLogging.logger { }
//
//        @JvmStatic
//        @Container
//        val mysqlMasterContainer: DockerComposeContainer<*> =
//            DockerComposeContainer(
//                File("docker-compose-for-integration-test.yml"),
//            ).apply {
//                withExposedService(
//                    MYSQL_MASTER_SERVICE_NAME,
//                    MYSQL_MASTER_SERVICE_PORT,
// //                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)),
//                )
//                start()
//            }
//
//        @JvmStatic
//        @Container
//        val mysqlSlaveContainer: DockerComposeContainer<*> =
//            DockerComposeContainer(
//                File("docker-compose-for-integration-test.yml"),
//            ).apply {
//                withExposedService(
//                    MYSQL_SLAVE_SERVICE_NAME,
//                    MYSQL_SLAVE_SERVICE_PORT,
// //                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)),
//                )
//                start()
//            }
//
//        @DynamicPropertySource
//        fun setProperties(registry: DynamicPropertyRegistry) {
//            val masterMappedPort =
//                mysqlMasterContainer.getServicePort(MYSQL_MASTER_SERVICE_NAME, MYSQL_MASTER_SERVICE_PORT)
//            val salveMappedPort =
//                mysqlSlaveContainer.getServicePort(MYSQL_MASTER_SERVICE_NAME, MYSQL_SLAVE_SERVICE_PORT)
//
//            logger.info { "MySql Master URL: ${mysqlMasterContainer.getServiceHost(MYSQL_MASTER_SERVICE_NAME, MYSQL_MASTER_SERVICE_PORT)}" }
//            logger.info { "MySQL Master mapped port: $masterMappedPort" }
//            logger.info { "MySql Slave URL: ${mysqlSlaveContainer.getServiceHost(MYSQL_SLAVE_SERVICE_NAME, MYSQL_SLAVE_SERVICE_PORT)}" }
//            logger.info { "MySQL Slave mapped port: $salveMappedPort" }
//
//            registry.apply {
//                add("spring.datasource.master.url") {
//                    String.format(
//                        "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
//                        masterMappedPort,
//                    )
//                }
//                add("spring.datasource.master.username") { MYSQL_USERNAME }
//                add("spring.datasource.master.password") { MYSQL_PASSWORD }
//
//                add("spring.datasource.slave.url") {
//                    String.format(
//                        "jdbc:mysql://localhost:%d/$MYSQL_DATABASE_NAME",
//                        salveMappedPort,
//                    )
//                }
//                add("spring.datasource.slave.username") { MYSQL_USERNAME }
//                add("spring.datasource.slave.password") { MYSQL_PASSWORD }
//            }
//        }
//    }
// }
