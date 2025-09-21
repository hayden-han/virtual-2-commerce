package kr.hhplus.be.server.domain.model.coupon.policy

import kr.hhplus.be.server.domain.exception.ConflictResourceException
import kr.hhplus.be.server.domain.model.coupon.CouponIssuance
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
            throw ConflictResourceException(
                message = "쿠폰 발급 기간이 아닙니다.",
                clue =
                    mapOf(
                        "쿠폰발급ID" to "${couponIssuance.id}",
                        "현재시간" to "$now",
                        "발급시작일" to "${couponIssuance.startAt}",
                        "발급종료일" to "${couponIssuance.endAt}",
                    ),
            )
        }

        if (!couponIssuance.hasRemainingQuota()) {
            throw ConflictResourceException(
                message = "쿠폰 발급 수량이 모두 소진되었습니다.",
                clue =
                    mapOf(
                        "쿠폰발급ID" to "${couponIssuance.id}",
                        "현재시간" to "$now",
                        "총발급수량" to "${couponIssuance.maxCount}",
                        "발급된수량" to "${couponIssuance.issuedCount}",
                    ),
            )
        }

        // TODO: 동일한 쿠폰을 이미 발급받았는지 확인
    }
}
