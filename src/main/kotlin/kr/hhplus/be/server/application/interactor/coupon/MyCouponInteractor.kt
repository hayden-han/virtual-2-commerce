package kr.hhplus.be.server.application.interactor.coupon

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.port.out.CouponOwnerOutput
import kr.hhplus.be.server.application.usecase.coupon.MyCouponUseCase
import kr.hhplus.be.server.application.vo.CouponItemVO
import kr.hhplus.be.server.domain.exception.NotFoundResourceException
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.member.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MyCouponInteractor(
    private val couponOwnerOutput: CouponOwnerOutput,
) : MyCouponUseCase {
    private val logger = KotlinLogging.logger { }

    @Transactional(readOnly = true)
    override fun getMyCoupons(memberId: Long): List<CouponItemVO> =
        couponOwnerOutput.findAllByMemberId(memberId).map {
            CouponItemVO(
                id = it.id!!,
                name = it.couponSummary.name,
                discountPercentage = it.couponSummary.discountPercentage,
                expiredAt = it.couponSummary.expiredAt,
                usingAt = it.usingAt,
            )
        }

    @Transactional
    override fun using(
        member: Member,
        couponSummaryId: Long,
        now: LocalDateTime,
    ): CouponOwner {
        val usingCouponOwner =
            couponOwnerOutput
                .findByIdAndMemberId(
                    couponSummaryId = couponSummaryId,
                    memberId = member.id!!,
                ).orElseThrow {
                    NotFoundResourceException(
                        message = "보유하지 않은 쿠폰입니다.",
                        clue = mapOf("memberId" to "${member.id}", "couponSummaryId" to "$couponSummaryId"),
                    )
                }.using(member, now)

        return couponOwnerOutput.save(usingCouponOwner)
    }
}
