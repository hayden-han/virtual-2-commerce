package kr.hhplus.be.server.infrastructure.persistence.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity
import java.time.LocalDateTime

@Entity
@Table(name = "coupon_issuance")
class CouponIssuanceJpaEntity(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_summary_id", nullable = false)
    val couponSummaryJpaEntity: CouponSummaryJpaEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val policyType: CouponIssuancePolicyType,
    @Column(nullable = false)
    val issuedCount: Int,
    @Column
    val maxCount: Int?,
    @Column(nullable = false)
    val startAt: LocalDateTime,
    @Column(nullable = false)
    val endAt: LocalDateTime,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
