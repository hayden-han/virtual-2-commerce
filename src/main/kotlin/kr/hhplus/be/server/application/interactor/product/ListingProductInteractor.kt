package kr.hhplus.be.server.application.interactor.product

import kr.hhplus.be.server.application.port.out.ListingProductOutput
import kr.hhplus.be.server.application.usecase.product.ListingProductUseCase
import kr.hhplus.be.server.domain.model.product.ProductSummary
import kr.hhplus.be.server.presentation.dto.product.ListingProductResponse
import kr.hhplus.be.server.presentation.dto.product.ProductSummaryItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ListingProductInteractor(
    private val listingProductOutput: ListingProductOutput,
) : ListingProductUseCase {
    override fun listingBy(
        page: Int,
        size: Int,
    ): ListingProductResponse {
        val productSummaries: List<ProductSummary> =
            listingProductOutput.listingBy(
                page = page,
                size = size,
            )

        return ListingProductResponse(
            rows = productSummaries.size,
            page = page,
            products =
                productSummaries.map {
                    ProductSummaryItem(
                        productId = it.id!!,
                        productName = it.name,
                        price = it.price,
                        stockQuantity = it.stockQuantity,
                    )
                },
        )
    }

    override fun topSellingProducts(): ListingProductResponse {
        val topSellingProductSummaries: List<ProductSummary> = listingProductOutput.topSellingProducts()
        return ListingProductResponse(
            rows = topSellingProductSummaries.size,
            page = 0,
            products =
                topSellingProductSummaries.map {
                    ProductSummaryItem(
                        productId = it.id!!,
                        productName = it.name,
                        price = it.price,
                        stockQuantity = it.stockQuantity,
                    )
                },
        )
    }
}
