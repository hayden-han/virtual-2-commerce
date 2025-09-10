package kr.hhplus.be.server.application.usecase.coupon

import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.presentation.dto.coupon.MyCouponsResponse
import java.time.LocalDateTime

interface MyCouponUseCase {
    fun getMyCoupons(memberId: Long): MyCouponsResponse

    fun using(
        member: Member,
        couponSummaryId: Long,
        now: LocalDateTime = LocalDateTime.now(),
    ): CouponOwner
}
