package kr.hhplus.be.server.domain.model.coupon

import kr.hhplus.be.server.domain.model.coupon.policy.CouponIssuancePolicy
import kr.hhplus.be.server.domain.model.member.Member
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 쿠폰 발급 상태. 쿠폰 정보에서 분리한 이유는
 * - 단일 책임 원칙: CouponSummary는 쿠폰의 기본 정보를 관리하는 역할이고, 발급은 별도의 비즈니스 로직.
 * - 확장성을 위한 의존성 분리: 향후 발급 정책에 선착순 외에도 추첨, 조건부 발급 등 다양한 발급 정책이 추가될 수 있는데, 정책에 변경이 있어도 CouponSummary는 영향받지 않게됨.
 */
data class CouponIssuance(
    val id: Long?,
    val couponSummary: CouponSummary,
    val policy: CouponIssuancePolicy,
    val issuedCount: Int = 0,
    val maxCount: Int?,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
) {
    fun tryIssue(
        member: Member,
        now: LocalDateTime = LocalDateTime.now(),
    ): CouponIssuance {
        // 발급 정책에 따른 추가 검증
        policy.canIssue(
            member = member,
            couponIssuance = this,
            now = now,
        )

        return copy(issuedCount = issuedCount + 1)
    }

    /**
     * 현재 시점에 발급이 가능한지 여부
     */
    internal fun isActive(now: LocalDateTime = LocalDateTime.now()): Boolean = !now.isBefore(startAt) && !now.isAfter(endAt)

    internal fun hasRemainingQuota(): Boolean = maxCount == null || issuedCount < maxCount
}
