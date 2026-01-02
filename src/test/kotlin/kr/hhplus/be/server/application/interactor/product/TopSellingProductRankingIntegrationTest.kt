package kr.hhplus.be.server.application.interactor.product

import kr.hhplus.be.server.application.listener.ProductSalesRankingEventListener
import kr.hhplus.be.server.application.usecase.product.TopSellingProductUseCase
import kr.hhplus.be.server.application.vo.PlaceOrderItemVO
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import kr.hhplus.be.server.common.annotation.IntegrationTest
import kr.hhplus.be.server.common.config.TestDataSourceConfig
import kr.hhplus.be.server.common.config.TestDockerComposeContainer
import kr.hhplus.be.server.domain.model.product.TopSellingProductQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import java.time.LocalDate

@IntegrationTest
@SpringBootTest
@Import(
    TestDockerComposeContainer::class,
    TestDataSourceConfig::class,
)
@SqlGroup(
    Sql(scripts = ["/sql/ranking-test-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/ranking-test-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class TopSellingProductRankingIntegrationTest {
    companion object {
        private const val RANKING_KEY_PREFIX = "product:sales:ranking"
        private val BASE_DATE = LocalDate.of(2025, 9, 19)
    }

    @Autowired
    private lateinit var topSellingProductUseCase: TopSellingProductUseCase

    @Autowired
    private lateinit var productSalesRankingEventListener: ProductSalesRankingEventListener

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @BeforeEach
    fun setup() {
        flushRankingData()
    }

    @AfterEach
    fun teardown() {
        flushRankingData()
    }

    private fun flushRankingData() {
        // 날짜별 키 패턴으로 삭제
        val keys = stringRedisTemplate.keys("$RANKING_KEY_PREFIX:*")
        if (keys.isNotEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @Nested
    @DisplayName("동일 상품 누적 판매")
    inner class CumulativeSalesTest {
        @Test
        @DisplayName("동일 상품에 대해 recordSales를 여러 번 호출하면 판매량이 누적된다")
        fun recordSales_accumulatesSalesForSameProduct() {
            // given
            val productId = 101L
            val salesDate = BASE_DATE
            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = salesDate)

            // when - 동일 상품에 대해 같은 날짜로 여러 번 판매 기록
            topSellingProductUseCase.recordSales(productId, 3, salesDate)
            topSellingProductUseCase.recordSales(productId, 5, salesDate)
            topSellingProductUseCase.recordSales(productId, 2, salesDate)

            // then - 판매량이 누적되어 조회됨
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            assertThat(result.products).hasSize(1)
            assertThat(result.products[0].id).isEqualTo(productId)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(10) // 3 + 5 + 2 = 10
        }

        @Test
        @DisplayName("여러 상품에 대해 누적 판매 시 정확한 순위가 반영된다")
        fun recordSales_multipleProductsCumulativeSales() {
            // given
            val salesDate = BASE_DATE
            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = salesDate)

            // when - 여러 상품에 대해 판매 기록
            // 상품 101: 총 6개 (2 + 4)
            topSellingProductUseCase.recordSales(101L, 2, salesDate)
            topSellingProductUseCase.recordSales(101L, 4, salesDate)

            // 상품 102: 총 10개 (3 + 7)
            topSellingProductUseCase.recordSales(102L, 3, salesDate)
            topSellingProductUseCase.recordSales(102L, 7, salesDate)

            // 상품 103: 총 5개 (5)
            topSellingProductUseCase.recordSales(103L, 5, salesDate)

            // then - 누적 판매량 순으로 정렬됨
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            assertThat(result.products).hasSize(3)
            assertThat(result.products[0].id).isEqualTo(102L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(10)
            assertThat(result.products[1].id).isEqualTo(101L)
            assertThat(result.products[1].totalOrderQuantity).isEqualTo(6)
            assertThat(result.products[2].id).isEqualTo(103L)
            assertThat(result.products[2].totalOrderQuantity).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("기간별 랭킹 조회")
    inner class DateRangeRankingTest {
        @Test
        @DisplayName("nDay=3일 때 최근 3일간의 판매량을 합산하여 조회한다")
        fun getTopSellingProducts_aggregatesMultipleDays() {
            // given
            val day1 = BASE_DATE.minusDays(2) // 9/17
            val day2 = BASE_DATE.minusDays(1) // 9/18
            val day3 = BASE_DATE              // 9/19

            // 각 날짜별 판매 기록
            topSellingProductUseCase.recordSales(101L, 5, day1)
            topSellingProductUseCase.recordSales(101L, 3, day2)
            topSellingProductUseCase.recordSales(101L, 2, day3)

            topSellingProductUseCase.recordSales(102L, 10, day2)

            val query = TopSellingProductQuery.of(nDay = 3, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 3일간 합산: 101=10, 102=10
            assertThat(result.products).hasSize(2)
            assertThat(result.products.map { it.totalOrderQuantity }).containsOnly(10)
        }

        @Test
        @DisplayName("조회 기간 외의 판매 데이터는 집계에서 제외된다")
        fun getTopSellingProducts_excludesOutOfRangeData() {
            // given
            val outsideRange = BASE_DATE.minusDays(5) // 범위 밖
            val insideRange = BASE_DATE               // 범위 안

            topSellingProductUseCase.recordSales(101L, 100, outsideRange) // 범위 밖 - 제외됨
            topSellingProductUseCase.recordSales(102L, 5, insideRange)    // 범위 안 - 포함됨

            val query = TopSellingProductQuery.of(nDay = 3, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 범위 밖 데이터는 제외
            assertThat(result.products).hasSize(1)
            assertThat(result.products[0].id).isEqualTo(102L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(5)
        }

        @Test
        @DisplayName("nDay=1일 때 당일 판매량만 조회한다")
        fun getTopSellingProducts_singleDayQuery() {
            // given
            val yesterday = BASE_DATE.minusDays(1)
            val today = BASE_DATE

            topSellingProductUseCase.recordSales(101L, 50, yesterday)
            topSellingProductUseCase.recordSales(102L, 10, today)

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 오늘 데이터만 조회
            assertThat(result.products).hasSize(1)
            assertThat(result.products[0].id).isEqualTo(102L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("Limit 초과 데이터")
    inner class LimitExceedingDataTest {
        @Test
        @DisplayName("랭킹 데이터가 limit보다 많을 때 limit 만큼만 반환된다")
        fun getTopSellingProducts_returnsOnlyLimitedItems() {
            // given - 10개 상품(101-110)에 대해 판매 기록
            val salesDate = BASE_DATE
            for (i in 101L..110L) {
                topSellingProductUseCase.recordSales(i, (111 - i).toInt(), salesDate)
            }

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 상위 5개만 반환
            assertThat(result.products).hasSize(5)
            assertThat(result.products.map { it.id }).containsExactly(101L, 102L, 103L, 104L, 105L)
            assertThat(result.products.map { it.totalOrderQuantity }).containsExactly(10, 9, 8, 7, 6)
        }

        @Test
        @DisplayName("랭킹 데이터가 limit보다 적을 때 있는 만큼만 반환된다")
        fun getTopSellingProducts_returnsAllWhenLessThanLimit() {
            // given - 3개 상품에 대해 판매 기록
            val salesDate = BASE_DATE
            topSellingProductUseCase.recordSales(101L, 10, salesDate)
            topSellingProductUseCase.recordSales(102L, 8, salesDate)
            topSellingProductUseCase.recordSales(103L, 5, salesDate)

            val query = TopSellingProductQuery.of(nDay = 1, limit = 10, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 3개만 반환
            assertThat(result.products).hasSize(3)
        }
    }

    @Nested
    @DisplayName("상품 정보 없는 랭킹")
    inner class RankingWithoutProductInfoTest {
        @Test
        @DisplayName("랭킹에 있지만 상품 정보가 DB에 없으면 해당 항목은 결과에서 제외된다")
        fun getTopSellingProducts_excludesProductsNotInDb() {
            // given
            val salesDate = BASE_DATE
            topSellingProductUseCase.recordSales(999L, 100, salesDate) // DB에 없는 상품 - 1위
            topSellingProductUseCase.recordSales(101L, 50, salesDate) // DB에 있는 상품 - 2위
            topSellingProductUseCase.recordSales(102L, 30, salesDate) // DB에 있는 상품 - 3위

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - DB에 없는 상품 999는 제외되고, 101과 102만 반환됨
            assertThat(result.products).hasSize(2)
            assertThat(result.products[0].id).isEqualTo(101L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(50)
            assertThat(result.products[1].id).isEqualTo(102L)
            assertThat(result.products[1].totalOrderQuantity).isEqualTo(30)
        }

        @Test
        @DisplayName("랭킹의 모든 상품이 DB에 없으면 빈 목록을 반환한다")
        fun getTopSellingProducts_emptyWhenAllProductsNotInDb() {
            // given - DB에 없는 상품들만 랭킹에 기록
            val salesDate = BASE_DATE
            topSellingProductUseCase.recordSales(9991L, 100, salesDate)
            topSellingProductUseCase.recordSales(9992L, 50, salesDate)

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then
            assertThat(result.products).isEmpty()
        }
    }

    @Nested
    @DisplayName("동점 처리")
    inner class TieBreakingTest {
        @Test
        @DisplayName("동일 판매량일 때 Redis Sorted Set의 기본 정렬(lexicographical)이 적용된다")
        fun getTopSellingProducts_tieBreakingByRedisDefault() {
            // given - 동일 판매량으로 기록
            val salesDate = BASE_DATE
            topSellingProductUseCase.recordSales(103L, 10, salesDate)
            topSellingProductUseCase.recordSales(101L, 10, salesDate)
            topSellingProductUseCase.recordSales(102L, 10, salesDate)

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then - 동일 점수일 때 Redis ZREVRANGE는 lexicographical 역순 정렬
            // "103" > "102" > "101" (문자열 비교)
            assertThat(result.products).hasSize(3)
            assertThat(result.products.map { it.totalOrderQuantity }).containsOnly(10)
            assertThat(result.products.map { it.id }).containsExactly(103L, 102L, 101L)
        }

        @Test
        @DisplayName("판매량이 다른 상품들 사이에 동점 상품이 있어도 정렬이 올바르게 동작한다")
        fun getTopSellingProducts_mixedTieBreaking() {
            // given
            val salesDate = BASE_DATE
            topSellingProductUseCase.recordSales(101L, 20, salesDate) // 1위
            topSellingProductUseCase.recordSales(105L, 15, salesDate) // 공동 2위
            topSellingProductUseCase.recordSales(103L, 15, salesDate) // 공동 2위
            topSellingProductUseCase.recordSales(102L, 10, salesDate) // 4위

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then
            assertThat(result.products).hasSize(4)
            assertThat(result.products[0].id).isEqualTo(101L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(20)
            // 공동 2위: 105 > 103 (lexicographical 역순)
            assertThat(result.products[1].id).isEqualTo(105L)
            assertThat(result.products[2].id).isEqualTo(103L)
            assertThat(result.products[3].id).isEqualTo(102L)
        }
    }

    @Nested
    @DisplayName("빈 랭킹 데이터")
    inner class EmptyRankingTest {
        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 목록을 반환한다")
        fun getTopSellingProducts_emptyWhenNoRankingData() {
            // given - 랭킹 데이터 없음 (flushRankingData()로 초기화됨)
            val query = TopSellingProductQuery.of(nDay = 3, limit = 5, curDate = BASE_DATE)

            // when
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            // then
            assertThat(result.products).isEmpty()
        }
    }

    @Nested
    @DisplayName("EventListener 통합테스트")
    inner class EventListenerIntegrationTest {
        @Test
        @DisplayName("주문 완료 이벤트가 발생하면 랭킹에 반영된다")
        fun handlePlaceOrderEvent_updatesRanking() {
            // given
            val orderDate = BASE_DATE
            val placeOrderResult = PlaceOrderResultVO(
                orderId = 1L,
                orderDate = orderDate,
                paymentMethod = "POINT",
                paymentChargeAmount = 50000L,
                paymentDiscountAmount = 0L,
                paymentTotalAmount = 50000L,
                orderItems = listOf(
                    PlaceOrderItemVO(productSummaryId = 101L, quantity = 3, price = 10000),
                    PlaceOrderItemVO(productSummaryId = 102L, quantity = 2, price = 20000),
                ),
            )

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = orderDate)

            // when - EventListener 직접 호출 (실제로는 @TransactionalEventListener로 호출됨)
            productSalesRankingEventListener.handlePlaceOrderEvent(placeOrderResult)

            // then - 랭킹에 반영됨
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            assertThat(result.products).hasSize(2)
            assertThat(result.products[0].id).isEqualTo(101L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(3)
            assertThat(result.products[1].id).isEqualTo(102L)
            assertThat(result.products[1].totalOrderQuantity).isEqualTo(2)
        }

        @Test
        @DisplayName("여러 주문 이벤트가 발생하면 랭킹이 누적된다")
        fun handlePlaceOrderEvent_accumulatesRanking() {
            // given
            val orderDate = BASE_DATE
            val firstOrder = PlaceOrderResultVO(
                orderId = 1L,
                orderDate = orderDate,
                paymentMethod = "POINT",
                paymentChargeAmount = 30000L,
                paymentDiscountAmount = 0L,
                paymentTotalAmount = 30000L,
                orderItems = listOf(
                    PlaceOrderItemVO(productSummaryId = 101L, quantity = 3, price = 10000),
                ),
            )

            val secondOrder = PlaceOrderResultVO(
                orderId = 2L,
                orderDate = orderDate,
                paymentMethod = "POINT",
                paymentChargeAmount = 40000L,
                paymentDiscountAmount = 0L,
                paymentTotalAmount = 40000L,
                orderItems = listOf(
                    PlaceOrderItemVO(productSummaryId = 101L, quantity = 2, price = 10000),
                    PlaceOrderItemVO(productSummaryId = 102L, quantity = 5, price = 20000),
                ),
            )

            val query = TopSellingProductQuery.of(nDay = 1, limit = 5, curDate = orderDate)

            // when - 두 개의 주문 이벤트 처리
            productSalesRankingEventListener.handlePlaceOrderEvent(firstOrder)
            productSalesRankingEventListener.handlePlaceOrderEvent(secondOrder)

            // then - 누적된 랭킹 확인
            val result = topSellingProductUseCase.getTopSellingProducts(query)

            assertThat(result.products).hasSize(2)
            // 101: 3 + 2 = 5, 102: 5 → 102가 1위 (lexicographical 역순)
            assertThat(result.products[0].id).isEqualTo(102L)
            assertThat(result.products[0].totalOrderQuantity).isEqualTo(5)
            assertThat(result.products[1].id).isEqualTo(101L)
            assertThat(result.products[1].totalOrderQuantity).isEqualTo(5)
        }
    }
}
