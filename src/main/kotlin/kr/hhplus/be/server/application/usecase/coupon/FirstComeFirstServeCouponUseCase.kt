package kr.hhplus.be.server.application.usecase.coupon

import kr.hhplus.be.server.presentation.dto.coupon.FirstComeFirstServeCouponsIssuanceResponse

interface FirstComeFirstServeCouponUseCase {
    fun issueCoupon(memberId: Long): FirstComeFirstServeCouponsIssuanceResponse
}
