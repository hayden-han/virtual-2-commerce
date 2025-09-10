package kr.hhplus.be.server.domain.model.payment

import kr.hhplus.be.server.application.vo.PlaceOrderPaymentSummaryVO
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.domain.model.order.OrderSummary

data class PaymentSummary(
    val id: Long? = null,
    val method: PaymentMethod,
    val totalAmount: Long,
    val discountAmount: Long,
    val chargeAmount: Long,
    val member: Member,
    val orderSummary: OrderSummary,
    val couponOwner: CouponOwner?,
) {
    /**
     * 유저가 요청한 결제정보와 생성된 결제정보가 일치하는지 검증
     */
    fun validate(requestPaymentSummary: PlaceOrderPaymentSummaryVO) {
        if (method != PaymentMethod.valueOf(requestPaymentSummary.method)) {
            throw IllegalStateException("결제수단이 일치하지 않습니다.")
        }
        if (totalAmount != requestPaymentSummary.totalAmount) {
            throw IllegalStateException("총 결제금액이 일치하지 않습니다.")
        }
        if (discountAmount != requestPaymentSummary.discountAmount) {
            throw IllegalStateException("할인 금액이 일치하지 않습니다.")
        }
        if (chargeAmount != requestPaymentSummary.chargeAmount) {
            throw IllegalStateException("실제 결제금액이 일치하지 않습니다.")
        }
    }

    companion object {
        /**
         * 결제정보 생성
         * - 주문 상품들의 총 금액 계산
         * - 쿠폰 적용 후 결제 금액 계산
         * - 결제수단, 회원, 주문정보 포함한 결제정보를 생성
         */
        fun createPaymentSummary(
            method: PaymentMethod,
            couponOwner: CouponOwner?,
            orderSummary: OrderSummary,
            member: Member,
        ): PaymentSummary {
            val totalAmount = orderSummary.getTotalAmount()
            val discountAmount =
                couponOwner?.let {
                    couponOwner.calculateDiscountAmount(totalAmount)
                } ?: 0L

            return PaymentSummary(
                method = method,
                totalAmount = totalAmount,
                discountAmount = discountAmount,
                chargeAmount = totalAmount - discountAmount,
                couponOwner = couponOwner,
                orderSummary = orderSummary,
                member = member,
            )
        }
    }
}

enum class PaymentMethod {
    POINT,
    CARD,
    PHONE,
    BANK_TRANSFER,
    VIRTUAL_ACCOUNT,
}
