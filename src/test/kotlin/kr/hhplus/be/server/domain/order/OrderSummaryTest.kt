package kr.hhplus.be.server.domain.order

import kr.hhplus.be.server.common.annotation.UnitTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@UnitTest
class OrderSummaryTest {
    @Nested
    @DisplayName("주문 생성")
    inner class PlaceOrderTest {
//        @Test
//        @DisplayName("주문 금액이 0원 이하인 경우 예외가 발생한다.")
//        fun placeOrderWithNonPositiveAmountThrowsException() {
//            // given
//            val memberId = 1L
//            val orderAmount = 0L // 0원 이하
//            val orderDate = LocalDateTime.of(2024, 6, 15, 12, 0)
//
//            // when & then
//            val exception =
//                assertThrows<IllegalArgumentException> {
//                    StubFactory.orderSummary(
//                        id = 1L,
//                        memberId = memberId,
//                        orderAmount = orderAmount,
//                        orderDate = orderDate,
//                    )
//                }
//            assertThat(exception.message).isEqualTo("주문 금액은 0원 이상이어야 합니다.")
//        }
    }
}
