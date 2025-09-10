package kr.hhplus.be.server.presentation.dto.product

data class ListingProductResponse(
    val rows: Int,
    val page: Int,
    val products: List<ProductSummaryItem>,
)

data class ProductSummaryItem(
    val productId: Long,
    val productName: String,
    val price: Int,
    val stockQuantity: Int,
)
