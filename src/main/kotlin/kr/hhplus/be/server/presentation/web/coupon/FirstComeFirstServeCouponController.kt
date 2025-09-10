package kr.hhplus.be.server.presentation.web.coupon

import kr.hhplus.be.server.application.interactor.coupon.FirstComeFirstServeCouponInteractor
import kr.hhplus.be.server.presentation.dto.coupon.FirstComeFirstServeCouponsIssuanceResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 선착순 쿠폰 관련 API
 */
@RestController
@RequestMapping("/api/v1/coupons/first-come-first-serve")
class FirstComeFirstServeCouponController(
    private val firstComeFirstServeCouponInteractor: FirstComeFirstServeCouponInteractor,
) {
    @PostMapping
    fun issuanceCoupon(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): FirstComeFirstServeCouponsIssuanceResponse =
        firstComeFirstServeCouponInteractor.issueCoupon(
            memberId = memberId,
        )
}
