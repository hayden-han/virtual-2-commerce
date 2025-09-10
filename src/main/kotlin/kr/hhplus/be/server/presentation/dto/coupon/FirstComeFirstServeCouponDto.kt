package kr.hhplus.be.server.presentation.dto.coupon

import java.time.LocalDateTime

data class FirstComeFirstServeCouponsIssuanceResponse(
    val id: Long,
    val name: String,
    val discountPercentage: Long,
    val expiredAt: LocalDateTime,
)
