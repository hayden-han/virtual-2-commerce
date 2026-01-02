package kr.hhplus.be.server.application.listener

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kr.hhplus.be.server.application.vo.PlaceOrderItemVO
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import kr.hhplus.be.server.infrastructure.config.KafkaTopicProperties
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.util.AopTestUtils
import java.time.LocalDate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@SpringJUnitConfig(PlaceOrderEventListenerTest.TestConfig::class)
class PlaceOrderEventListenerTest {
    @Autowired
    private lateinit var placeOrderEventListener: PlaceOrderEventListener

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, PlaceOrderResultVO>

    private val listenerSpy: PlaceOrderEventListener
        get() = AopTestUtils.getTargetObject(placeOrderEventListener)

    @AfterEach
    fun tearDown() {
        clearMocks(listenerSpy, kafkaTemplate)
    }

    private val placeOrderResult =
        PlaceOrderResultVO(
            orderId = 1L,
            orderDate = LocalDate.of(2025, 9, 19),
            paymentMethod = "POINT",
            paymentChargeAmount = 1000L,
            paymentDiscountAmount = 0L,
            paymentTotalAmount = 1000L,
            orderItems = listOf(PlaceOrderItemVO(quantity = 1, price = 1000, productSummaryId = 1L)),
        )

    private fun createSuccessFuture(): CompletableFuture<SendResult<String, PlaceOrderResultVO>> {
        val recordMetadata = RecordMetadata(TopicPartition("place-order-complete", 0), 0, 0, 0, 0, 0)
        val producerRecord = ProducerRecord<String, PlaceOrderResultVO>("place-order-complete", placeOrderResult)
        val sendResult = SendResult(producerRecord, recordMetadata)
        return CompletableFuture.completedFuture(sendResult)
    }

    @Test
    @DisplayName("재시도 없이 주문완료 이벤트를 Kafka로 발행한다")
    fun `handlePlaceOrderEvent succeeds without retry`() {
        every { kafkaTemplate.send(any<String>(), any(), any()) } returns createSuccessFuture()

        placeOrderEventListener.handlePlaceOrderEvent(placeOrderResult)

        verify(exactly = 1) { kafkaTemplate.send("place-order-complete", "1", placeOrderResult) }
        verify(exactly = 1) { listenerSpy.handlePlaceOrderEvent(placeOrderResult) }
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 재시도 후 성공하면 재시도를 더 이상 하지 않는다")
    fun `handlePlaceOrderEvent retries up to success`() {
        every { kafkaTemplate.send(any<String>(), any(), any()) } throws RuntimeException("first") andThenThrows
            RuntimeException("second") andThen createSuccessFuture()

        placeOrderEventListener.handlePlaceOrderEvent(placeOrderResult)

        verify(exactly = 3) { kafkaTemplate.send("place-order-complete", "1", placeOrderResult) }
        verify(exactly = 3) { listenerSpy.handlePlaceOrderEvent(placeOrderResult) }
    }

    @Test
    @DisplayName("모든 재시도 후 실패하면 복구 로직이 실행된다")
    fun `handlePlaceOrderEvent retries up to recover`() {
        every { kafkaTemplate.send(any<String>(), any(), any()) } throws RuntimeException("failure")

        placeOrderEventListener.handlePlaceOrderEvent(placeOrderResult)

        verify(exactly = 1) { listenerSpy.recoverPlaceOrderEvent(any(), placeOrderResult) }
        verify(exactly = 3) { kafkaTemplate.send("place-order-complete", "1", placeOrderResult) }
        verify(exactly = 3) { listenerSpy.handlePlaceOrderEvent(placeOrderResult) }
    }

    @TestConfiguration
    @EnableRetry(proxyTargetClass = true)
    @EnableAsync(proxyTargetClass = true)
    class TestConfig : AsyncConfigurer {
        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, PlaceOrderResultVO> = mockk(relaxed = true)

        @Bean
        fun kafkaTopicProperties(): KafkaTopicProperties =
            KafkaTopicProperties(
                groupId = "virtual-2-commerce-group",
                topics = KafkaTopicProperties.Topics(placeOrderComplete = "place-order-complete"),
            )

        @Bean
        fun placeOrderEventListener(
            kafkaTemplate: KafkaTemplate<String, PlaceOrderResultVO>,
            kafkaTopicProperties: KafkaTopicProperties,
        ): PlaceOrderEventListener = spyk(PlaceOrderEventListener(kafkaTemplate, kafkaTopicProperties))

        override fun getAsyncExecutor(): Executor = SyncTaskExecutor()
    }
}
