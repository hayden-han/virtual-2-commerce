package kr.hhplus.be.server.domain.model.product

/**
 * 인기 판매 상품 랭킹을 나타내는 도메인 모델
 *
 * Redis Sorted Set을 데이터 원천으로 하며, 랭킹 내 순서와 상품 정보 결합을 캡슐화한다.
 * UseCase는 내부 순서 관리 로직을 알 필요 없이 결과만 조회할 수 있다.
 */
data class TopSellingProductRanking(
    private val entries: List<RankingEntry>,
) {
    val size: Int
        get() = entries.size

    fun isEmpty(): Boolean = entries.isEmpty()

    /**
     * 랭킹에 포함된 상품 ID 목록을 순위 순서대로 반환한다.
     */
    fun getProductIds(): List<Long> = entries.map { it.productId }

    /**
     * 상품 정보를 결합하여 인기 상품 목록을 생성한다.
     * 랭킹 순서를 유지하며, 상품 정보가 없는 항목은 제외된다.
     */
    fun withProducts(products: List<ProductSummary>): TopSellingProducts {
        if (entries.isEmpty()) {
            return TopSellingProducts.empty()
        }

        val productMap = products.associateBy { it.id }

        val topSellingProducts = entries.mapNotNull { entry ->
            productMap[entry.productId]?.let { product ->
                TopSellingProduct(
                    product = product,
                    salesCount = entry.salesCount,
                    rank = entry.rank,
                )
            }
        }

        return TopSellingProducts(items = topSellingProducts)
    }

    /**
     * 랭킹 내 개별 항목
     */
    data class RankingEntry(
        val productId: Long,
        val salesCount: Int,
        val rank: Int,
    ) {
        init {
            require(salesCount >= 0) { "판매 수량은 0 이상이어야 합니다." }
            require(rank > 0) { "순위는 1 이상이어야 합니다." }
        }
    }

    companion object {
        /**
         * 원시 랭킹 데이터로부터 생성한다.
         * 입력 순서가 곧 순위가 된다. (1위부터 시작)
         */
        fun from(rawData: List<Pair<Long, Int>>): TopSellingProductRanking {
            val entries = rawData.mapIndexed { index, (productId, salesCount) ->
                RankingEntry(
                    productId = productId,
                    salesCount = salesCount,
                    rank = index + 1,
                )
            }
            return TopSellingProductRanking(entries = entries)
        }

        fun empty(): TopSellingProductRanking = TopSellingProductRanking(entries = emptyList())
    }
}
