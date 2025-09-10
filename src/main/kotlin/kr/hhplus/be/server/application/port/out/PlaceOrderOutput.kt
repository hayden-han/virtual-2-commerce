package kr.hhplus.be.server.application.port.out

import kr.hhplus.be.server.domain.model.order.OrderSummary

interface PlaceOrderOutput {
    fun saveOrder(orderSummary: OrderSummary): OrderSummary
}
