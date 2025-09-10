package kr.hhplus.be.server.domain.model.order

import kr.hhplus.be.server.domain.model.member.Member

data class OrderSummary(
    val id: Long?,
    val memberId: Long,
    val orderItems: List<OrderItem>,
) {
    fun getTotalAmount(): Long = orderItems.sumOf { (it.price * it.quantity).toLong() }

    companion object {
        /**
         * 주문 생성
         */
        fun placeOrder(
            memberId: Long,
            orderItems: List<OrderItem>,
        ): OrderSummary =
            OrderSummary(
                id = null,
                memberId = memberId,
                orderItems = orderItems,
            )
    }
}
