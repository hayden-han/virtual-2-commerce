package kr.hhplus.be.server.application.usecase.product

import kr.hhplus.be.server.presentation.dto.product.ListingProductResponse
import org.springframework.stereotype.Service

interface ListingProductUseCase {
    fun listingBy(
        page: Int,
        size: Int,
    ): ListingProductResponse

    fun topSellingProducts(): ListingProductResponse
}
