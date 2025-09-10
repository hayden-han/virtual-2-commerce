package kr.hhplus.be.server.application.interactor.coupon

import kr.hhplus.be.server.application.port.out.CouponOwnerOutput
import kr.hhplus.be.server.application.usecase.coupon.MyCouponUseCase
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.presentation.dto.coupon.CouponItem
import kr.hhplus.be.server.presentation.dto.coupon.MyCouponsResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MyCouponInteractor(
    private val couponOwnerOutput: CouponOwnerOutput,
) : MyCouponUseCase {
    // TODO: 임시 데이터를 반환한다. 추후 DB에서 조회하도록 변경
    @Transactional(readOnly = true)
    override fun getMyCoupons(memberId: Long): MyCouponsResponse =
        MyCouponsResponse(
            coupons =
                listOf(
                    CouponItem(
                        id = 1L,
                        name = "10% 할인 쿠폰",
                        discountPercentage = 10L,
                        expiredAt = LocalDateTime.of(2025, 12, 31, 23, 59, 59),
                        usingAt = LocalDateTime.of(2025, 8, 27, 13, 0, 0),
                    ),
                ),
        )

    @Transactional
    override fun using(
        member: Member,
        couponSummaryId: Long,
        now: LocalDateTime,
    ): CouponOwner =
        couponOwnerOutput
            .findByIdAndMemberId(
                couponSummaryId = couponSummaryId,
                memberId = member.id!!,
            ).orElseThrow {
                IllegalStateException("CouponOwner not found for memberId: $member and couponSummaryId: $couponSummaryId")
            }.using(member, now)
            .let(couponOwnerOutput::save)
}
