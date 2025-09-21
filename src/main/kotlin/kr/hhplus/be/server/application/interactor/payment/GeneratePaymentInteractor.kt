package kr.hhplus.be.server.application.interactor.payment

import kr.hhplus.be.server.application.port.out.PaymentOutput
import kr.hhplus.be.server.application.usecase.payment.GeneratePaymentUseCase
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.domain.model.order.OrderSummary
import kr.hhplus.be.server.domain.model.payment.PaymentSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GeneratePaymentInteractor(
    private val paymentOutput: PaymentOutput,
) : GeneratePaymentUseCase {
    /**
     * 1. 쿠폰 소유 상태 조회
     * 2. 쿠폰 및 주문 상품들의 정보로 결제정보를 생성
     */
    @Transactional
    override fun generatePaymentSummary(
        member: Member,
        couponOwner: CouponOwner?,
        orderSummary: OrderSummary,
        method: String,
    ): PaymentSummary {
        val paymentSummary =
            PaymentSummary.createPaymentSummary(
                method = method,
                couponOwner = couponOwner,
                orderSummary = orderSummary,
                member = member,
            )

        return paymentOutput.save(paymentSummary)
    }
}
