package kr.hhplus.be.server.domain.model.coupon.policy

import kr.hhplus.be.server.domain.model.coupon.CouponIssuance
import kr.hhplus.be.server.domain.model.member.Member
import java.time.LocalDateTime

/**
 * 발급 정책 인터페이스
 */
interface CouponIssuancePolicy {
    fun canIssue(
        member: Member,
        couponIssuance: CouponIssuance,
        now: LocalDateTime = LocalDateTime.now(),
    )
}
