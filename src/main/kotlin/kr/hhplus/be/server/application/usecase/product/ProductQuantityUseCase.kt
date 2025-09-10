package kr.hhplus.be.server.application.usecase.product

import kr.hhplus.be.server.domain.model.order.OrderItem
import kr.hhplus.be.server.domain.model.product.ProductSummary
import org.springframework.stereotype.Service

interface ProductQuantityUseCase {
    fun reduceBy(orderItems: List<OrderItem>): List<ProductSummary>
}
