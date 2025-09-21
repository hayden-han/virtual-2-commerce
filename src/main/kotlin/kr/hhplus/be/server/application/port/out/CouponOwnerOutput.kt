package kr.hhplus.be.server.application.port.out

import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.presentation.dto.coupon.CouponItem
import java.util.Optional

interface CouponOwnerOutput {
    fun findByIdAndMemberId(
        couponSummaryId: Long,
        memberId: Long,
    ): Optional<CouponOwner>

    fun findAllByMemberId(memberId: Long): List<CouponOwner>

    fun save(couponOwner: CouponOwner): CouponOwner
}
