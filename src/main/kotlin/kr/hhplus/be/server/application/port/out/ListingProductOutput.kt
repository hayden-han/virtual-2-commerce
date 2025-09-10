package kr.hhplus.be.server.application.port.out

import kr.hhplus.be.server.domain.model.product.ProductSummary

interface ListingProductOutput {
    fun listingBy(
        page: Int,
        size: Int,
    ): List<ProductSummary>

    fun topSellingProducts(): List<ProductSummary>
}
