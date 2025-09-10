package kr.hhplus.be.server.domain.model.coupon

import java.time.LocalDateTime

data class CouponSummary(
    val id: Long?,
    val name: String,
    val discountPercentage: Long,
    val expiredAt: LocalDateTime,
) {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean = now.isAfter(expiredAt)
}
