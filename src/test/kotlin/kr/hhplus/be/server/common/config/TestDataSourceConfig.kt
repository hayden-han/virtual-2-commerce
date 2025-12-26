package kr.hhplus.be.server.common.config

import com.zaxxer.hikari.HikariDataSource
import kr.hhplus.be.server.infrastructure.persistence.config.DataSourceType
import kr.hhplus.be.server.infrastructure.persistence.config.DynamicRoutingDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import javax.sql.DataSource

@Profile("test")
@Configuration
class TestDataSourceConfig {
    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun masterDataSource(): HikariDataSource =
        DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .url(TestDockerComposeContainer.getMasterJdbcUrl())
            .username(TestDockerComposeContainer.MYSQL_USERNAME)
            .password(TestDockerComposeContainer.MYSQL_PASSWORD)
            .build()

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun slaveDataSource(): HikariDataSource =
        DataSourceBuilder
            .create()
            .type(HikariDataSource::class.java)
            .url(TestDockerComposeContainer.getSlaveJdbcUrl())
            .username(TestDockerComposeContainer.MYSQL_USERNAME)
            .password(TestDockerComposeContainer.MYSQL_PASSWORD)
            .build()

    @Bean
    fun routingDataSource(
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        @Qualifier("slaveDataSource") slaveDataSource: DataSource,
    ): DataSource {
        val dynamicRoutingDataSource = DynamicRoutingDataSource()
        val targetDataSources: Map<Any, Any> =
            mapOf(
                DataSourceType.MASTER to masterDataSource,
                DataSourceType.SLAVE to slaveDataSource,
            )
        dynamicRoutingDataSource.setTargetDataSources(targetDataSources)
        dynamicRoutingDataSource.setDefaultTargetDataSource(masterDataSource)

        return dynamicRoutingDataSource
    }

    @DependsOn("routingDataSource")
    @Primary
    @Bean
    fun dataSource(
        @Qualifier("routingDataSource") dataSource: DataSource,
    ): DataSource = LazyConnectionDataSourceProxy(dataSource)

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun redisConnectionFactory(): RedisConnectionFactory =
        LettuceConnectionFactory(
            TestDockerComposeContainer.getRedisHost(),
            TestDockerComposeContainer.getRedisMappedPort(),
        )

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun testProducerFactory(): ProducerFactory<String, PlaceOrderResultVO> {
        val props = mutableMapOf<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = TestDockerComposeContainer.getKafkaBootstrapServers()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.RETRIES_CONFIG] = 3
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun testKafkaTemplate(): KafkaTemplate<String, PlaceOrderResultVO> =
        KafkaTemplate(testProducerFactory())

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun testConsumerFactory(): ConsumerFactory<String, PlaceOrderResultVO> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = TestDockerComposeContainer.getKafkaBootstrapServers()
        props[ConsumerConfig.GROUP_ID_CONFIG] = "virtual-2-commerce-group"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

        val jsonDeserializer = JsonDeserializer(PlaceOrderResultVO::class.java)
        jsonDeserializer.addTrustedPackages("kr.hhplus.be.server.application.vo")
        jsonDeserializer.setUseTypeHeaders(false)

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            jsonDeserializer,
        )
    }

    @Bean
    @DependsOn("TestDockerComposeContainer")
    fun testKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, PlaceOrderResultVO> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, PlaceOrderResultVO>()
        factory.consumerFactory = testConsumerFactory()
        return factory
    }
}
