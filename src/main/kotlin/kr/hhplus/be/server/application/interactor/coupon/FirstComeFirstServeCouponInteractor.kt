package kr.hhplus.be.server.application.interactor.coupon

import kr.hhplus.be.server.presentation.dto.coupon.FirstComeFirstServeCouponsIssuanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.Long

@Service
class FirstComeFirstServeCouponInteractor {
    @Transactional
    fun issueCoupon(memberId: Long): FirstComeFirstServeCouponsIssuanceResponse {
        /*
        TODO: 선착순 쿠폰 발급로직 구현
         */
        return FirstComeFirstServeCouponsIssuanceResponse(
            id = 1L,
            name = "SPRING_SALE",
            discountPercentage = 10L,
            expiredAt = LocalDateTime.of(2026, 3, 18, 13, 0),
        )
    }
}
