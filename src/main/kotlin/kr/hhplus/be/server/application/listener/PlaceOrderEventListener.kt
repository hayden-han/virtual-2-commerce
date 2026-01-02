package kr.hhplus.be.server.application.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import kr.hhplus.be.server.infrastructure.config.KafkaTopicProperties
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PlaceOrderEventListener(
    private val kafkaTemplate: KafkaTemplate<String, PlaceOrderResultVO>,
    private val kafkaTopicProperties: KafkaTopicProperties,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 이미 기존에 기 적용된 application event를 활용한 이벤트 기반 설계
     * 트랜잭션이 성공적으로 커밋된 후에 이벤트를 처리한다.
     * 메인 트랜잭션과 분리하기 위해 비동기로 실행하며,
     * Kafka 메시지 발행 실패 시 최대 2회(총 3회)까지 재시도한다.
     * 최종 실패 시 비즈니스 플로우에 영향을 주지 않도록 경고 로그만 남긴다.
     */
    @Async
    @Retryable(
        retryFor = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 100, multiplier = 1.5),
    )
    @TransactionalEventListener
    fun handlePlaceOrderEvent(placeOrderResult: PlaceOrderResultVO) {
        logger.debug { "주문완료 이벤트 핸들링 - Kafka 메시지 발행: $placeOrderResult" }
        val topic = kafkaTopicProperties.topics.placeOrderComplete
        kafkaTemplate.send(topic, placeOrderResult.orderId.toString(), placeOrderResult)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error(ex) { "Kafka 메시지 발행 실패: topic=$topic, orderId=${placeOrderResult.orderId}" }
                } else {
                    logger.debug { "Kafka 메시지 발행 성공: topic=$topic, offset=${result.recordMetadata.offset()}" }
                }
            }
    }

    @Recover
    fun recoverPlaceOrderEvent(
        exception: Exception,
        placeOrderResult: PlaceOrderResultVO,
    ) {
        logger.warn(exception) { "주문완료 이벤트 처리 실패 - 최대 재시도 횟수 초과 placeOrderResult=$placeOrderResult" }
    }
}
