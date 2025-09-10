package kr.hhplus.be.server.infrastructure.persistence.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.order.OrderSummary
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity

@Entity
@Table(name = "order_summary")
class OrderSummaryJpaEntity(
    @Column(nullable = false)
    val memberId: Long,
    @OneToMany(mappedBy = "orderSummary", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val orderItems: MutableList<OrderItemJpaEntity>,
) : CreatedAndUpdatedAtAuditEntity() {
    fun addOrderItems(orderItemList: List<OrderItemJpaEntity>) {
        this.orderItems.addAll(orderItemList)
    }

    fun toDomain(): OrderSummary =
        OrderSummary(
            id = this.id,
            memberId = memberId,
            orderItems = this.orderItems.map { it.toDomain() },
        )

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        fun from(domain: OrderSummary): OrderSummaryJpaEntity {
            val orderSummaryJpaEntity =
                OrderSummaryJpaEntity(
                    memberId = domain.memberId,
                    orderItems = mutableListOf(),
                ).apply {
                    this.id = domain.id
                }
            val orderItemEntities =
                domain.orderItems.map {
                    OrderItemJpaEntity(
                        orderSummary = orderSummaryJpaEntity,
                        productSummaryId = it.productSummaryId,
                        quantity = it.quantity,
                        price = it.price,
                    ).apply {
                        this.id = it.id
                    }
                }
            orderSummaryJpaEntity.addOrderItems(orderItemEntities)

            return orderSummaryJpaEntity
        }
    }
}
