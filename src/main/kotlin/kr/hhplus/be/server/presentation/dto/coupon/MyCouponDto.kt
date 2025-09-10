package kr.hhplus.be.server.presentation.dto.coupon

import java.time.LocalDateTime

data class MyCouponsResponse(
    val coupons: List<CouponItem>,
)

data class CouponItem(
    val id: Long,
    val name: String,
    val discountPercentage: Long,
    val expiredAt: LocalDateTime,
    val usingAt: LocalDateTime,
)
