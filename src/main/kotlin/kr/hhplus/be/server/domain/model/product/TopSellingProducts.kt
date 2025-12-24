package kr.hhplus.be.server.domain.model.product

/**
 * 인기 판매 상품 목록을 나타내는 도메인 모델
 *
 * TopSellingProductRanking에서 상품 정보와 결합된 결과를 담는다.
 */
data class TopSellingProducts(
    val items: List<TopSellingProduct>,
) {
    val size: Int
        get() = items.size

    fun isEmpty(): Boolean = items.isEmpty()

    companion object {
        fun empty(): TopSellingProducts = TopSellingProducts(items = emptyList())
    }
}
