package kr.hhplus.be.server.domain.model.product

/**
 * 인기 판매 상품 정보를 나타내는 도메인 모델
 *
 * ProductSummary와 판매 랭킹 정보를 결합한 결과
 */
data class TopSellingProduct(
    val product: ProductSummary,
    val salesCount: Int,
    val rank: Int,
) {
    val productId: Long
        get() = product.id!!

    val productName: String
        get() = product.name

    val price: Int
        get() = product.price

    val stockQuantity: Int
        get() = product.stockQuantity
}
