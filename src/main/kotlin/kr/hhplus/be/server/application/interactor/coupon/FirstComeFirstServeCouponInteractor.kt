package kr.hhplus.be.server.application.interactor.coupon

import kr.hhplus.be.server.presentation.dto.coupon.FirstComeFirstServeCouponsIssuanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FirstComeFirstServeCouponInteractor {
    @Transactional
    fun issueCoupon(memberId: Long): FirstComeFirstServeCouponsIssuanceResponse =
        FirstComeFirstServeCouponsIssuanceResponse(
            id = 1L,
            name = "10% 할인 쿠폰",
            discountPercentage = 10L,
            expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
        )
}
