package kr.hhplus.be.server.application.usecase.product

import kr.hhplus.be.server.application.vo.TopSellingProductVO
import kr.hhplus.be.server.domain.model.product.TopSellingProductQuery
import java.time.LocalDate

interface TopSellingProductUseCase {
    fun getTopSellingProducts(query: TopSellingProductQuery): TopSellingProductVO

    fun recordSales(
        productId: Long,
        quantity: Int,
        salesDate: LocalDate,
    )
}
