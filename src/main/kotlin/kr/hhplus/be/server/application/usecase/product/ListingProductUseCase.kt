package kr.hhplus.be.server.application.usecase.product

import kr.hhplus.be.server.application.vo.ListingProductVO

interface ListingProductUseCase {
    fun listingBy(
        page: Int,
        size: Int,
        sortBy: String,
        descending: String,
    ): ListingProductVO
}
