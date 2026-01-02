package kr.hhplus.be.server.application.port.out

import kr.hhplus.be.server.domain.model.product.TopSellingProductRanking
import java.time.LocalDate

/**
 * 상품 판매 랭킹 저장소를 위한 출력 포트
 */
interface ProductSalesRankingOutput {
    /**
     * 특정 날짜의 상품 판매량을 증가시킨다
     * @param productId 상품 ID
     * @param quantity 판매 수량
     * @param salesDate 판매 날짜
     */
    fun incrementSalesCount(
        productId: Long,
        quantity: Int,
        salesDate: LocalDate,
    )

    /**
     * 특정 기간의 상위 판매 상품 랭킹을 조회한다
     * @param startDate 시작 날짜 (포함)
     * @param endDate 종료 날짜 (포함)
     * @param limit 조회할 상품 수
     * @return 인기 상품 랭킹 도메인 모델
     */
    fun getTopRanking(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): TopSellingProductRanking
}
