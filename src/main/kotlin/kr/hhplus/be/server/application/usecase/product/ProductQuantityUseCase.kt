package kr.hhplus.be.server.application.usecase.product

import kr.hhplus.be.server.application.vo.PlaceOrderItemVO

interface ProductQuantityUseCase {
    fun reduceBy(orderItems: List<PlaceOrderItemVO>)
}
