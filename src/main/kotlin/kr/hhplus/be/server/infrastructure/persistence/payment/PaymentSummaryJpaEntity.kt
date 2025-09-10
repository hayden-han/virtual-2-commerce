package kr.hhplus.be.server.infrastructure.persistence.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.payment.PaymentSummary
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity
import kr.hhplus.be.server.infrastructure.persistence.coupon.CouponOwnerJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.member.MemberJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.order.OrderSummaryJpaEntity

@Entity
@Table(name = "payment_summary")
class PaymentSummaryJpaEntity(
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val method: PaymentMethod,
    @Column(nullable = false)
    val totalAmount: Long,
    @Column(nullable = false)
    val discountAmount: Long,
    @Column(nullable = false)
    val chargeAmount: Long,
    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    val member: MemberJpaEntity,
    @OneToOne
    @JoinColumn(name = "order_summary_id", nullable = false)
    val orderSummary: OrderSummaryJpaEntity,
    @OneToOne
    @JoinColumn(name = "coupon_owner_id")
    val couponOwner: CouponOwnerJpaEntity?,
) : CreatedAndUpdatedAtAuditEntity() {
    fun toDomain(): PaymentSummary =
        PaymentSummary(
            id = this.id,
            method =
                kr.hhplus.be.server.domain.model.payment.PaymentMethod
                    .valueOf(this.method.name),
            totalAmount = this.totalAmount,
            discountAmount = this.discountAmount,
            chargeAmount = this.chargeAmount,
            member = this.member.toDomain(),
            orderSummary = this.orderSummary.toDomain(),
            couponOwner = this.couponOwner?.toDomain(),
        )

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        fun from(domain: PaymentSummary): PaymentSummaryJpaEntity =
            PaymentSummaryJpaEntity(
                method = PaymentMethod.valueOf(domain.method.name),
                totalAmount = domain.totalAmount,
                discountAmount = domain.discountAmount,
                chargeAmount = domain.chargeAmount,
                member = MemberJpaEntity.from(domain.member),
                orderSummary = OrderSummaryJpaEntity.from(domain.orderSummary),
                couponOwner = domain.couponOwner?.let { CouponOwnerJpaEntity.from(it) },
            ).apply {
                this.id = domain.id
            }
    }
}

enum class PaymentMethod {
    POINT,
    CARD,
    PHONE,
    BANK_TRANSFER,
    VIRTUAL_ACCOUNT,
}
