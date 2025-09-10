package kr.hhplus.be.server.domain.model.coupon.policy

import kr.hhplus.be.server.domain.model.coupon.CouponIssuance
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import java.time.LocalDateTime

/**
 * 선착순 발급 정책
 */
class FirstComeFirstServedPolicy : CouponIssuancePolicy {
    override fun canIssue(
        member: Member,
        couponIssuance: CouponIssuance,
        now: LocalDateTime,
    ) {
        if (!couponIssuance.isActive(now)) {
            throw IllegalStateException("쿠폰 발급 기간이 아닙니다.")
        }

        if (!couponIssuance.hasRemainingQuota()) {
            throw IllegalStateException("쿠폰 발급 수량이 모두 소진되었습니다.")
        }

        // TODO: 동일한 쿠폰을 이미 발급받았는지 확인
    }
}
