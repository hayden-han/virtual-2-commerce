package kr.hhplus.be.server.domain.model.product

import java.time.LocalDate

/**
 * 인기 상품 조회를 위한 쿼리 조건을 나타내는 도메인 VO
 *
 * 생성 시점에 유효성 검증을 수행하여 항상 유효한 조회 조건만 존재하도록 보장한다.
 */
data class TopSellingProductQuery(
    val nDay: Int,
    val limit: Int,
    val curDate: LocalDate = LocalDate.now(),
) {
    init {
        require(nDay > 0) { "조회 기간은 0보다 커야합니다. (nDay: $nDay)" }
        require(limit > 0) { "조회 갯수는 0보다 커야합니다. (limit: $limit)" }
    }

    companion object {
        fun of(
            nDay: Int,
            limit: Int,
            curDate: LocalDate = LocalDate.now(),
        ): TopSellingProductQuery = TopSellingProductQuery(nDay = nDay, limit = limit, curDate = curDate)
    }
}
