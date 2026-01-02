package kr.hhplus.be.server.application.interactor.product

import kr.hhplus.be.server.application.enums.ListingProductDescending
import kr.hhplus.be.server.application.enums.ListingProductSortBy
import kr.hhplus.be.server.application.port.out.ListingProductOutput
import kr.hhplus.be.server.application.usecase.product.ListingProductUseCase
import kr.hhplus.be.server.application.vo.ListingProductVO
import kr.hhplus.be.server.application.vo.ProductSummaryItemVO
import kr.hhplus.be.server.domain.model.product.ProductSummary
import kr.hhplus.be.server.infrastructure.config.RedisCacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ListingProductInteractor(
    private val listingProductOutput: ListingProductOutput,
) : ListingProductUseCase {
    @Cacheable(
        value = [RedisCacheConfig.CACHE_PRODUCTS],
        key = "#page + '_' + #size + '_' + #sortBy + '_' + #descending",
    )
    override fun listingBy(
        page: Int,
        size: Int,
        sortBy: String,
        descending: String,
    ): ListingProductVO {
        val listingProductSortBy =
            ListingProductSortBy.from(sortBy)
                ?: throw IllegalArgumentException("지원하지않는 정렬기준입니다($sortBy)")
        val listingProductDescending =
            ListingProductDescending.from(descending)
                ?: throw IllegalArgumentException("지원하지않는 정렬차순입니다($descending)")

        val productSummaries: List<ProductSummary> =
            listingProductOutput.listingBy(
                page = page,
                size = size,
                sortBy = listingProductSortBy,
                descending = listingProductDescending,
            )

        return ListingProductVO(
            rows = productSummaries.size,
            page = page,
            products =
                productSummaries.map {
                    ProductSummaryItemVO(
                        id = it.id!!,
                        name = it.name,
                        price = it.price,
                        stockQuantity = it.stockQuantity,
                    )
                },
        )
    }
}
