package kr.hhplus.be.server.domain.model.coupon

import kr.hhplus.be.server.domain.exception.ConflictResourceException
import kr.hhplus.be.server.domain.model.member.Member
import java.time.LocalDateTime

data class CouponOwner(
    val id: Long?,
    val couponSummary: CouponSummary,
    val memberId: Long,
    val usingAt: LocalDateTime?,
) {
    fun calculateDiscountAmount(totalAmount: Long): Long {
        if (totalAmount <= 0) return 0L // 주문 금액이 0원 이하인 경우 할인 적용 안함
        return totalAmount * couponSummary.discountPercentage / 100
    }

    fun using(
        member: Member,
        now: LocalDateTime = LocalDateTime.now(),
    ): CouponOwner {
        checkUsable(member, now)

        return copy(
            id = id,
            couponSummary = couponSummary,
            memberId = memberId,
            usingAt = now,
        )
    }

    /**
     * 쿠폰 사용 가능여부
     */
    fun checkUsable(
        member: Member,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        if (member.id != memberId) {
            throw ConflictResourceException(
                message = "사용하려는 유저에게 발급된 쿠폰이 아닙니다.",
                clue = mapOf("요청 유저ID" to "${member.id}", "쿠폰을 보유한 유저ID" to "$memberId"),
            )
        }

        if (usingAt != null) {
            throw ConflictResourceException(message = "이미 사용된 쿠폰입니다.")
        }

        if (couponSummary.isExpired(now)) {
            throw ConflictResourceException("만료된 쿠폰입니다.")
        }
    }
}
