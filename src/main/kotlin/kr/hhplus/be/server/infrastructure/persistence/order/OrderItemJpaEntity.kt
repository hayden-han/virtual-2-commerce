package kr.hhplus.be.server.infrastructure.persistence.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.order.OrderItem
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity

@Entity
@Table(name = "order_item")
class OrderItemJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_summary_id", nullable = false)
    val orderSummary: OrderSummaryJpaEntity,
    @Column(nullable = false)
    val productSummaryId: Long,
    @Column(nullable = false)
    val quantity: Int,
    @Column(nullable = false)
    val price: Int,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    fun toDomain() =
        OrderItem(
            id = this.id,
            orderSummaryId = this.orderSummary.id!!,
            productSummaryId = this.productSummaryId,
            quantity = this.quantity,
            price = this.price,
        )
}
