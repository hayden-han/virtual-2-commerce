package kr.hhplus.be.server.application.vo

import kr.hhplus.be.server.domain.model.product.TopSellingProduct
import kr.hhplus.be.server.domain.model.product.TopSellingProducts

data class ListingProductVO(
    val rows: Int,
    val page: Int,
    val products: List<ProductSummaryItemVO>,
)

data class ProductSummaryItemVO(
    val id: Long,
    val name: String,
    val price: Int,
    val stockQuantity: Int,
)

data class TopSellingProductVO(
    val products: List<TopSellingProductItemVO>,
) {
    companion object {
        fun from(domain: TopSellingProducts): TopSellingProductVO =
            TopSellingProductVO(
                products = domain.items.map { TopSellingProductItemVO.from(it) },
            )

        fun empty(): TopSellingProductVO = TopSellingProductVO(products = emptyList())
    }
}

data class TopSellingProductItemVO(
    val id: Long,
    val name: String,
    val price: Int,
    val stockQuantity: Int,
    val totalOrderQuantity: Int,
) {
    companion object {
        fun from(domain: TopSellingProduct): TopSellingProductItemVO =
            TopSellingProductItemVO(
                id = domain.productId,
                name = domain.productName,
                price = domain.price,
                stockQuantity = domain.stockQuantity,
                totalOrderQuantity = domain.salesCount,
            )
    }
}
