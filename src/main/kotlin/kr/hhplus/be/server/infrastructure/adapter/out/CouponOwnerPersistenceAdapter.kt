package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.CouponOwnerOutput
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.infrastructure.persistence.coupon.CouponOwnerJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.coupon.CouponOwnerJpaRepository
import kr.hhplus.be.server.infrastructure.persistence.coupon.CouponSummaryJpaEntity
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class CouponOwnerPersistenceAdapter(
    private val couponOwnerJpaRepository: CouponOwnerJpaRepository,
) : CouponOwnerOutput {
    override fun findByIdAndMemberId(
        couponSummaryId: Long,
        memberId: Long,
    ): Optional<CouponOwner> =
        couponOwnerJpaRepository
            .findByIdAndMemberId(couponSummaryId, memberId)
            .map(CouponOwnerJpaEntity::toDomain)

    override fun save(couponOwner: CouponOwner): CouponOwner {
        val entity =
            CouponOwnerJpaEntity(
                memberId = couponOwner.memberId,
                couponSummaryJpaEntity = CouponSummaryJpaEntity.from(couponOwner.couponSummary),
            ).apply {
                id = couponOwner.id
                usingAt = couponOwner.usingAt
            }

        return couponOwnerJpaRepository
            .save(entity)
            .toDomain()
    }
}
