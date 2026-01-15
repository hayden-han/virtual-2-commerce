package kr.hhplus.be.server.application.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.port.out.ExternalServiceOutput
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class PlaceOrderKafkaConsumer(
    private val externalServiceOutput: ExternalServiceOutput,
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["\${kafka.topics.place-order-complete}"],
        groupId = "\${kafka.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
    )
    fun consumePlaceOrderEvent(placeOrderResult: PlaceOrderResultVO) {
        logger.info { "Kafka 메시지 수신: orderId=${placeOrderResult.orderId}" }
        try {
            externalServiceOutput.call(placeOrderResult)
            logger.debug { "외부 서비스 호출 성공: orderId=${placeOrderResult.orderId}" }
        } catch (e: Exception) {
            logger.error(e) { "외부 서비스 호출 실패: orderId=${placeOrderResult.orderId}" }
            throw e
        }
    }
}
