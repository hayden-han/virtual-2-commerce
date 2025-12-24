package kr.hhplus.be.server.application.interactor.product

import kr.hhplus.be.server.application.port.out.ProductSalesRankingOutput
import kr.hhplus.be.server.application.port.out.ProductSummaryOutput
import kr.hhplus.be.server.application.usecase.product.TopSellingProductUseCase
import kr.hhplus.be.server.application.vo.TopSellingProductVO
import kr.hhplus.be.server.domain.model.product.TopSellingProductQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TopSellingProductInteractor(
    private val productSalesRankingOutput: ProductSalesRankingOutput,
    private val productSummaryOutput: ProductSummaryOutput,
) : TopSellingProductUseCase {
    @Transactional(readOnly = true)
    override fun getTopSellingProducts(query: TopSellingProductQuery): TopSellingProductVO {
        // nDay 기간에 해당하는 날짜 범위 계산
        val endDate = query.curDate
        val startDate = endDate.minusDays(query.nDay.toLong() - 1)

        // 랭킹 도메인 모델 조회
        val ranking = productSalesRankingOutput.getTopRanking(startDate, endDate, query.limit)

        if (ranking.isEmpty()) {
            return TopSellingProductVO.empty()
        }

        // 랭킹에서 상품 ID 추출 및 상품 정보 조회
        val products = productSummaryOutput.findAllInIds(ranking.getProductIds())

        // 도메인 모델에서 상품 정보 결합 (순서 관리는 도메인이 담당)
        val topSellingProducts = ranking.withProducts(products)

        return TopSellingProductVO.from(topSellingProducts)
    }

    override fun recordSales(
        productId: Long,
        quantity: Int,
        salesDate: LocalDate,
    ) {
        productSalesRankingOutput.incrementSalesCount(productId, quantity, salesDate)
    }
}
