package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.PlaceOrderOutput
import kr.hhplus.be.server.domain.model.order.OrderSummary
import kr.hhplus.be.server.infrastructure.persistence.order.OrderItemJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.order.OrderSummaryJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.order.OrderSummaryJpaOutput
import org.springframework.stereotype.Component

@Component
class OrderPersistenceAdapter(
    private val orderSummaryJpaRepository: OrderSummaryJpaOutput,
) : PlaceOrderOutput {
    override fun saveOrder(orderSummary: OrderSummary): OrderSummary {
        val orderSummaryJpaEntity =
            OrderSummaryJpaEntity(
                memberId = orderSummary.memberId,
                orderItems = mutableListOf(),
            )

        val orderItemJpaEntities =
            orderSummary.orderItems
                .map {
                    OrderItemJpaEntity(
                        orderSummary = orderSummaryJpaEntity,
                        productSummaryId = it.productSummaryId,
                        quantity = it.quantity,
                        price = it.price,
                    )
                }

        orderSummaryJpaEntity.addOrderItems(orderItemJpaEntities.toMutableList())

        return orderSummaryJpaRepository
            .save(orderSummaryJpaEntity)
            .let {
                orderSummary.copy(
                    id = it.id,
                    orderItems = orderItemJpaEntities.map { entity -> entity.toDomain() },
                )
            }
    }
}
