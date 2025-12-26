package kr.hhplus.be.server.infrastructure.config

import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties::class)
class KafkaConfig(
    private val kafkaProperties: KafkaProperties,
    private val kafkaTopicProperties: KafkaTopicProperties,
) {
    @Bean
    fun producerFactory(): ProducerFactory<String, PlaceOrderResultVO> {
        val props = mutableMapOf<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        props[ProducerConfig.ACKS_CONFIG] = kafkaProperties.producer.acks ?: "all"
        props[ProducerConfig.RETRIES_CONFIG] = kafkaProperties.producer.retries ?: 3
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, PlaceOrderResultVO> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, PlaceOrderResultVO> {
        val props = mutableMapOf<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = kafkaTopicProperties.groupId
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = kafkaProperties.consumer.autoOffsetReset ?: "earliest"

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
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, PlaceOrderResultVO> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, PlaceOrderResultVO>()
        factory.consumerFactory = consumerFactory()
        return factory
    }
}
