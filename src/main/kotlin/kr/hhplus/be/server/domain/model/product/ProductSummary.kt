package kr.hhplus.be.server.domain.model.product

data class ProductSummary(
    val id: Long?,
    val name: String,
    val price: Int,
    val stockQuantity: Int,
) {
    fun reduceStockQuantity(quantity: Int): ProductSummary {
        if (quantity <= 0) throw IllegalArgumentException("주문수량은 0개보다 많아야합니다")
        if (stockQuantity < quantity) {
            throw IllegalArgumentException("'$name($id)' 상품의 재고가 부족합니다.(현재 재고: $stockQuantity, 요청 수량: $quantity)")
        }

        return copy(
            id = id,
            name = name,
            price = price,
            stockQuantity = stockQuantity - quantity,
        )
    }

    fun increaseStockQuantity(quantity: Int): ProductSummary {
        if (quantity <= 0) throw IllegalArgumentException("추가수량은 0개보다 많아야합니다")

        return copy(
            id = id,
            name = name,
            price = price,
            stockQuantity = stockQuantity + quantity,
        )
    }
}
