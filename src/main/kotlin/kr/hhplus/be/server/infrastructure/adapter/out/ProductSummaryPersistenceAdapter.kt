package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.ListingProductOutput
import kr.hhplus.be.server.application.port.out.ProductSummaryOutput
import kr.hhplus.be.server.domain.model.product.ProductSummary
import kr.hhplus.be.server.infrastructure.persistence.product.ProductSummaryJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.product.ProductSummaryJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductSummaryPersistenceAdapter(
    private val productSummaryRepository: ProductSummaryJpaRepository,
) : ProductSummaryOutput,
    ListingProductOutput {
    override fun findAllInIds(productSummaryIds: Collection<Long>): List<ProductSummary> =
        productSummaryRepository
            .findAllByIdIn(productSummaryIds)
            .map { productSummary -> productSummary.toDomain() }

    override fun saveAll(updatedProductSummaryList: Collection<ProductSummary>): List<ProductSummary> {
        val updatedProductSummaryJpaEntityList =
            updatedProductSummaryList
                .map { productSummary -> ProductSummaryJpaEntity.from(productSummary) }

        return productSummaryRepository
            .saveAll(updatedProductSummaryJpaEntityList)
            .map { productSummaryJpaEntity -> productSummaryJpaEntity.toDomain() }
    }

    override fun listingBy(
        page: Int,
        size: Int,
    ): List<ProductSummary> {
        val pageable = PageRequest.of(page, size)
        return productSummaryRepository
            .findAll(pageable)
            .content
            .map { productSummaryJpaEntity -> productSummaryJpaEntity.toDomain() }
    }

    override fun topSellingProducts(): List<ProductSummary> {
        // TODO: 추후 구현
        //        productSummaryRepository
        //            .findTop10ByOrderByTotalSalesQuantityDesc()
        //            .map { productSummaryJpaEntity -> productSummaryJpaEntity.toDomain() }
        return emptyList()
    }
}
