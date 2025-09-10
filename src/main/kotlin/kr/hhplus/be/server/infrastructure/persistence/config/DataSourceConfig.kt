package kr.hhplus.be.server.infrastructure.persistence.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import javax.sql.DataSource

@Configuration
class DataSourceConfig {
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    fun masterDataSourceProperties() = DataSourceProperties()

    @Bean
    @ConfigurationProperties("spring.datasource.master.hikari")
    fun masterDataSource(
        @Qualifier("masterDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()

    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    fun slaveDataSourceProperties() = DataSourceProperties()

    @Bean
    @ConfigurationProperties("spring.datasource.slave.hikari")
    fun slaveDataSource(
        @Qualifier("slaveDataSourceProperties") properties: DataSourceProperties,
    ): DataSource = properties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()

    @Bean
    @Primary
    fun routingDataSource(
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        @Qualifier("slaveDataSource") slaveDataSource: DataSource,
    ): DataSource {
        val routingDataSource = RoutingDataSource()
        val targetDataSources =
            mapOf(
                DataSourceType.MASTER to masterDataSource,
                DataSourceType.SLAVE to slaveDataSource,
            )
        routingDataSource.setTargetDataSources(targetDataSources as Map<Any, Any>)
        routingDataSource.setDefaultTargetDataSource(masterDataSource)
        return routingDataSource
    }
}
