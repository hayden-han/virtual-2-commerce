package kr.hhplus.be.server.application.interactor.balance

import kr.hhplus.be.server.application.port.out.MemberOutput
import kr.hhplus.be.server.application.port.out.MyBalanceOutput
import kr.hhplus.be.server.application.usecase.balance.MyBalanceUseCase
import kr.hhplus.be.server.application.usecase.order.GenerateOrderUseCase
import kr.hhplus.be.server.application.usecase.order.PlaceOrderUseCase
import kr.hhplus.be.server.application.usecase.payment.GeneratePaymentUseCase
import kr.hhplus.be.server.application.vo.PlaceOrderItemVO
import kr.hhplus.be.server.application.vo.PlaceOrderPaymentSummaryVO
import kr.hhplus.be.server.common.annotation.IntegrationTest
import kr.hhplus.be.server.common.config.NoOpEventPublisherConfig
import kr.hhplus.be.server.utils.TransactionUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import java.time.LocalDateTime

@IntegrationTest
@SpringBootTest
@Import(NoOpEventPublisherConfig::class)
class MyBalanceInteractorIntegrationTest {
    @Autowired
    private lateinit var myBalanceUseCase: MyBalanceUseCase

    @Autowired
    private lateinit var myBalanceOutput: MyBalanceOutput

    @Autowired
    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    @Nested
    @DisplayName("잔고 차감(reduceMyBalance)")
    @SqlGroup(
        Sql(value = ["/sql/my-balance-interactor-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        Sql(value = ["/sql/my-balance-interactor-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
    )
    inner class ReduceMyBalance {
        @Test
        @DisplayName("잔고 차감이 정상적으로 처리되고 DB에 반영된다")
        fun reduceMyBalance_Success() {
            // given
            val memberId = 5001L
            val initialBalance = 100000L
            val reduceAmount = 30000L
            val expectedBalance = initialBalance - reduceAmount

            // when
            val result =
                myBalanceUseCase.reduceMyBalance(
                    memberId = memberId,
                    amount = reduceAmount,
                )

            // then - 반환값 검증
            assertThat(result.balance).isEqualTo(expectedBalance)
            assertThat(result.member.id).isEqualTo(memberId)

            // then - DB에 실제로 저장되었는지 검증
            val savedBalance = myBalanceOutput.findByMemberId(memberId).get()
            assertThat(savedBalance.balance)
                .describedAs("DB에서 조회한 잔액이 차감된 금액과 일치해야 합니다")
                .isEqualTo(expectedBalance)
        }

        @Test
        @DisplayName("여러 번 잔고를 차감해도 정상적으로 누적 처리된다")
        fun reduceMyBalance_MultipleReductions_Success() {
            // given
            val memberId = 5001L
            val initialBalance = 100000L
            val firstReduceAmount = 20000L
            val secondReduceAmount = 30000L
            val expectedBalance = initialBalance - firstReduceAmount - secondReduceAmount

            // when
            myBalanceUseCase.reduceMyBalance(memberId = memberId, amount = firstReduceAmount)
            myBalanceUseCase.reduceMyBalance(memberId = memberId, amount = secondReduceAmount)

            // then - DB에 실제로 저장되었는지 검증
            val savedBalance = myBalanceOutput.findByMemberId(memberId).get()
            assertThat(savedBalance.balance)
                .describedAs("두 번 차감 후 잔액이 올바르게 계산되어야 합니다")
                .isEqualTo(expectedBalance)
        }
    }

    @Nested
    @DisplayName("PlaceOrderFacade를 통한 잔고 차감")
    @SqlGroup(
        Sql(value = ["/sql/place-order-facade-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        Sql(value = ["/sql/place-order-facade-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
    )
    inner class PlaceOrderBalanceDeduction {
        @Autowired
        private lateinit var memberOutput: MemberOutput

        @Autowired
        private lateinit var transactionUtils: TransactionUtils

        @Test
        @DisplayName("PlaceOrderFacade를 직접 호출해도 잔고가 정상적으로 차감된다")
        fun placeOrder_DirectCall_BalanceDeducted() {
            // given
            val memberId = 5101L
            val initialBalance = 2000000L
            val chargeAmount = 1903000L
            val expectedBalance = initialBalance - chargeAmount

            val orderItems =
                listOf(
                    PlaceOrderItemVO(productSummaryId = 5101L, quantity = 1, price = 1790000),
                    PlaceOrderItemVO(productSummaryId = 5102L, quantity = 1, price = 89000),
                    PlaceOrderItemVO(productSummaryId = 5103L, quantity = 2, price = 12000),
                )
            val paymentSummary =
                PlaceOrderPaymentSummaryVO(
                    method = "POINT",
                    totalAmount = 1903000L,
                    discountAmount = 0L,
                    chargeAmount = chargeAmount,
                )

            // when
            val result =
                placeOrderUseCase.placeOrder(
                    memberId = memberId,
                    couponSummaryId = null,
                    orderItems = orderItems,
                    requestPaymentSummary = paymentSummary,
                    orderAt = LocalDateTime.of(2025, 6, 18, 12, 59, 59),
                )

            // then - 주문 결과 검증
            assertThat(result.orderId).isNotNull()

            // then - DB에서 잔고 검증
            val savedBalance =
                transactionUtils.onPrimaryTransaction {
                    myBalanceOutput.findByMemberId(memberId).get()
                }
            assertThat(savedBalance.balance)
                .describedAs("PlaceOrderFacade 호출 후 잔액이 차감되어야 합니다")
                .isEqualTo(expectedBalance)
        }

        @Test
        @DisplayName("PlaceOrderFacade를 트랜잭션 내에서 호출해도 잔고가 정상적으로 차감된다")
        fun placeOrder_WithinTransaction_BalanceDeducted() {
            // given
            val memberId = 5101L
            val initialBalance = 2000000L
            val chargeAmount = 1903000L
            val expectedBalance = initialBalance - chargeAmount

            val orderItems =
                listOf(
                    PlaceOrderItemVO(productSummaryId = 5101L, quantity = 1, price = 1790000),
                    PlaceOrderItemVO(productSummaryId = 5102L, quantity = 1, price = 89000),
                    PlaceOrderItemVO(productSummaryId = 5103L, quantity = 2, price = 12000),
                )
            val paymentSummary =
                PlaceOrderPaymentSummaryVO(
                    method = "POINT",
                    totalAmount = 1903000L,
                    discountAmount = 0L,
                    chargeAmount = chargeAmount,
                )

            // when - transactionUtils 내에서 placeOrderUseCase 호출
            val result =
                transactionUtils.onPrimaryTransaction {
                    placeOrderUseCase.placeOrder(
                        memberId = memberId,
                        couponSummaryId = null,
                        orderItems = orderItems,
                        requestPaymentSummary = paymentSummary,
                        orderAt = LocalDateTime.of(2025, 6, 18, 12, 59, 59),
                    )
                }

            // then - 주문 결과 검증
            assertThat(result.orderId).isNotNull()

            // then - DB에서 잔고 검증
            val savedBalance =
                transactionUtils.onPrimaryTransaction {
                    myBalanceOutput.findByMemberId(memberId).get()
                }
            assertThat(savedBalance.balance)
                .describedAs("트랜잭션 내에서 PlaceOrderFacade 호출 후 잔액이 차감되어야 합니다")
                .isEqualTo(expectedBalance)
        }

        @Test
        @DisplayName("findByIdWithLock 호출 후 reduceMyBalance를 호출해도 잔고가 정상적으로 차감된다")
        fun reduceMyBalance_AfterFindByIdWithLock_BalanceDeducted() {
            // given
            val memberId = 5101L
            val initialBalance = 2000000L
            val reduceAmount = 100000L
            val expectedBalance = initialBalance - reduceAmount

            // when - findByIdWithLock 먼저 호출하고 reduceMyBalance 호출 (PlaceOrderFacade가 하는 것처럼)
            val result =
                transactionUtils.onPrimaryTransaction {
                    val member = memberOutput.findByIdWithLock(memberId).get()
                    assertThat(member).isNotNull()

                    myBalanceUseCase.reduceMyBalance(
                        memberId = memberId,
                        amount = reduceAmount,
                    )
                }

            // then - 반환값 검증
            assertThat(result.balance).isEqualTo(expectedBalance)

            // then - DB에서 잔고 검증
            val savedBalance =
                transactionUtils.onPrimaryTransaction {
                    myBalanceOutput.findByMemberId(memberId).get()
                }
            assertThat(savedBalance.balance)
                .describedAs("findByIdWithLock 후 reduceMyBalance 호출 시 잔액이 차감되어야 합니다")
                .isEqualTo(expectedBalance)
        }

        @Autowired
        private lateinit var generateOrderUseCase: GenerateOrderUseCase

        @Test
        @DisplayName("findByIdWithLock + generateOrder + reduceMyBalance 조합 테스트")
        fun reduceMyBalance_AfterFindByIdWithLockAndGenerateOrder_BalanceDeducted() {
            // given
            val memberId = 5101L
            val initialBalance = 2000000L
            val reduceAmount = 100000L
            val expectedBalance = initialBalance - reduceAmount

            val orderItems = listOf(
                PlaceOrderItemVO(productSummaryId = 5101L, quantity = 1, price = 1790000),
            )

            // when - PlaceOrderFacade의 흐름을 따라가며 테스트
            val result =
                transactionUtils.onPrimaryTransaction {
                    // Step 1: findByIdWithLock
                    val member = memberOutput.findByIdWithLock(memberId).get()

                    // Step 2: generateOrder
                    val orderSummary = generateOrderUseCase.generateOrder(
                        member = member,
                        orderItems = orderItems,
                    )
                    assertThat(orderSummary.id).isNotNull()

                    // Step 3: reduceMyBalance
                    myBalanceUseCase.reduceMyBalance(
                        memberId = memberId,
                        amount = reduceAmount,
                    )
                }

            // then - 반환값 검증
            assertThat(result.balance).isEqualTo(expectedBalance)

            // then - DB에서 잔고 검증
            val savedBalance =
                transactionUtils.onPrimaryTransaction {
                    myBalanceOutput.findByMemberId(memberId).get()
                }
            assertThat(savedBalance.balance)
                .describedAs("findByIdWithLock + generateOrder 후 reduceMyBalance 호출 시 잔액이 차감되어야 합니다")
                .isEqualTo(expectedBalance)
        }

        @Autowired
        private lateinit var generatePaymentUseCase: GeneratePaymentUseCase

        @Test
        @DisplayName("findByIdWithLock + generateOrder + generatePaymentSummary + reduceMyBalance 조합 테스트")
        fun reduceMyBalance_AfterFullFlow_BalanceDeducted() {
            // given
            val memberId = 5101L
            val initialBalance = 2000000L
            val reduceAmount = 1790000L
            val expectedBalance = initialBalance - reduceAmount

            val orderItems = listOf(
                PlaceOrderItemVO(productSummaryId = 5101L, quantity = 1, price = 1790000),
            )

            // when - PlaceOrderFacade의 전체 흐름을 따라가며 테스트 (쿠폰 제외)
            val result =
                transactionUtils.onPrimaryTransaction {
                    // Step 1: findByIdWithLock
                    val member = memberOutput.findByIdWithLock(memberId).get()

                    // Step 2: generateOrder
                    val orderSummary = generateOrderUseCase.generateOrder(
                        member = member,
                        orderItems = orderItems,
                    )

                    // Step 3: generatePaymentSummary
                    val paymentSummary = generatePaymentUseCase.generatePaymentSummary(
                        coupon = null,
                        orderSummary = orderSummary,
                        method = "POINT",
                    )
                    assertThat(paymentSummary.chargeAmount).isEqualTo(reduceAmount)

                    // Step 4: reduceMyBalance
                    myBalanceUseCase.reduceMyBalance(
                        memberId = memberId,
                        amount = paymentSummary.chargeAmount,
                    )
                }

            // then - 반환값 검증
            assertThat(result.balance).isEqualTo(expectedBalance)

            // then - DB에서 잔고 검증
            val savedBalance =
                transactionUtils.onPrimaryTransaction {
                    myBalanceOutput.findByMemberId(memberId).get()
                }
            assertThat(savedBalance.balance)
                .describedAs("전체 흐름 후 reduceMyBalance 호출 시 잔액이 차감되어야 합니다")
                .isEqualTo(expectedBalance)
        }
    }
}
