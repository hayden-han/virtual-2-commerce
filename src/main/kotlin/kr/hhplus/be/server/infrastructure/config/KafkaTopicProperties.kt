package kr.hhplus.be.server.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka")
data class KafkaTopicProperties(
    val groupId: String,
    val topics: Topics,
) {
    data class Topics(
        val placeOrderComplete: String,
    )
}
