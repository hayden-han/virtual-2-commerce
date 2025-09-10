import kr.hhplus.be.server.application.vo.PlaceOrderItemVO
import kr.hhplus.be.server.application.vo.PlaceOrderPaymentSummaryVO
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import java.time.LocalDateTime

interface PlaceOrderInput {
    fun placeOrder(
        memberId: Long,
        couponSummaryId: Long?,
        orderItems: List<PlaceOrderItemVO>,
        requestPaymentSummary: PlaceOrderPaymentSummaryVO,
        orderAt: LocalDateTime = LocalDateTime.now(),
    ): PlaceOrderResultVO
}
