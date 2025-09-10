package kr.hhplus.be.server.infrastructure.persistence.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.coupon.CouponSummary
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_summary")
class CouponSummaryJpaEntity(
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val discountPercentage: Long,
    @Column(nullable = false)
    val expiredAt: LocalDateTime,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    fun toDomain() =
        CouponSummary(
            id = id,
            name = name,
            discountPercentage = discountPercentage,
            expiredAt = expiredAt,
        )

    companion object {
        fun from(domain: CouponSummary) =
            CouponSummaryJpaEntity(
                name = domain.name,
                discountPercentage = domain.discountPercentage,
                expiredAt = domain.expiredAt,
            ).apply {
                id = domain.id
            }
    }
}
