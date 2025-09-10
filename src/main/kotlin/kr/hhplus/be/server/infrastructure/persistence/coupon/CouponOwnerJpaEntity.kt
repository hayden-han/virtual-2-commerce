package kr.hhplus.be.server.infrastructure.persistence.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_owner")
class CouponOwnerJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_summary_id", nullable = false)
    val couponSummaryJpaEntity: CouponSummaryJpaEntity,
    @Column(nullable = false)
    val memberId: Long,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = true)
    var usingAt: LocalDateTime? = null

    fun toDomain() =
        CouponOwner(
            id = id,
            couponSummary = couponSummaryJpaEntity.toDomain(),
            memberId = memberId,
            usingAt = usingAt,
        )

    companion object {
        fun from(domain: CouponOwner): CouponOwnerJpaEntity =
            CouponOwnerJpaEntity(
                couponSummaryJpaEntity = CouponSummaryJpaEntity.from(domain.couponSummary),
                memberId = domain.memberId,
            ).apply {
                this.id = domain.id
                this.usingAt = domain.usingAt
            }
    }
}
