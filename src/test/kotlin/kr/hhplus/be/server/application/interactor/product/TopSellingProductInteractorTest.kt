package kr.hhplus.be.server.application.interactor.product

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.hhplus.be.server.application.port.out.ProductSalesRankingOutput
import kr.hhplus.be.server.application.port.out.ProductSummaryOutput
import kr.hhplus.be.server.application.vo.TopSellingProductItemVO
import kr.hhplus.be.server.common.annotation.UnitTest
import kr.hhplus.be.server.domain.model.product.TopSellingProductQuery
import kr.hhplus.be.server.domain.model.product.TopSellingProductRanking
import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@UnitTest
class TopSellingProductInteractorTest {
    private val productSalesRankingOutputMock = mockk<ProductSalesRankingOutput>()
    private val productSummaryOutputMock = mockk<ProductSummaryOutput>()
    private val topSellingProductInteractor =
        TopSellingProductInteractor(
            productSalesRankingOutput = productSalesRankingOutputMock,
            productSummaryOutput = productSummaryOutputMock,
        )

    @Nested
    @DisplayName("가장 많이 팔린 상품 조회")
    inner class GetTopSellingProductsTest {
        @Test
        @DisplayName("가장 많이 팔린 상품이 정상적으로 반환된다")
        fun getTopSellingProducts() {
            // given
            val curDate = LocalDate.of(2025, 9, 19)
            val query = TopSellingProductQuery.of(nDay = 3, limit = 2, curDate = curDate)
            val startDate = curDate.minusDays(2) // nDay=3이면 오늘 포함 3일
            val ranking = TopSellingProductRanking.from(
                listOf(
                    1L to 10,
                    2L to 8,
                ),
            )
            val productSummaries =
                listOf(
                    StubFactory.productSummary(
                        id = 1L,
                        name = "Apple iPhone 15 Pro",
                        price = 1700000,
                        stockQuantity = 5,
                    ),
                    StubFactory.productSummary(
                        id = 2L,
                        name = "Samsung Galaxy S24 Ultra",
                        price = 1600000,
                        stockQuantity = 3,
                    ),
                )
            every {
                productSalesRankingOutputMock.getTopRanking(startDate, curDate, query.limit)
            } returns ranking
            every {
                productSummaryOutputMock.findAllInIds(listOf(1L, 2L))
            } returns productSummaries

            // when
            val result = topSellingProductInteractor.getTopSellingProducts(query)

            // then
            assertThat(result.products).hasSize(2)
            assertThat(result.products).containsExactly(
                TopSellingProductItemVO(
                    id = 1L,
                    name = "Apple iPhone 15 Pro",
                    price = 1700000,
                    stockQuantity = 5,
                    totalOrderQuantity = 10,
                ),
                TopSellingProductItemVO(
                    id = 2L,
                    name = "Samsung Galaxy S24 Ultra",
                    price = 1600000,
                    stockQuantity = 3,
                    totalOrderQuantity = 8,
                ),
            )
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 목록을 반환한다")
        fun getTopSellingProducts_emptyRanking() {
            // given
            val curDate = LocalDate.of(2025, 9, 19)
            val query = TopSellingProductQuery.of(nDay = 3, limit = 5, curDate = curDate)
            val startDate = curDate.minusDays(2)
            every {
                productSalesRankingOutputMock.getTopRanking(startDate, curDate, query.limit)
            } returns TopSellingProductRanking.empty()

            // when
            val result = topSellingProductInteractor.getTopSellingProducts(query)

            // then
            assertThat(result.products).isEmpty()
        }

        @Test
        @DisplayName("기간이 0 이하이면 예외가 발생한다")
        fun getTopSellingProducts_invalidNDay() {
            // given
            val invalidNDays = listOf(0, -1, -10)

            for (nDay in invalidNDays) {
                // when & then
                assertThatThrownBy {
                    TopSellingProductQuery.of(nDay = nDay, limit = 5)
                }.isInstanceOf(
                    IllegalArgumentException::class.java,
                ).hasMessageContaining("조회 기간은 0보다 커야합니다")
            }
        }

        @Test
        @DisplayName("갯수가 0 이하이면 예외가 발생한다")
        fun getTopSellingProducts_invalidLimit() {
            // given
            val invalidLimits = listOf(0, -1, -10)

            for (limit in invalidLimits) {
                // when & then
                assertThatThrownBy {
                    TopSellingProductQuery.of(nDay = 3, limit = limit)
                }.isInstanceOf(
                    IllegalArgumentException::class.java,
                ).hasMessageContaining("조회 갯수는 0보다 커야합니다")
            }
        }
    }

    @Nested
    @DisplayName("판매 기록")
    inner class RecordSalesTest {
        @Test
        @DisplayName("판매 기록이 정상적으로 저장된다")
        fun recordSales() {
            // given
            val productId = 1L
            val quantity = 5
            val salesDate = LocalDate.of(2025, 9, 19)
            every {
                productSalesRankingOutputMock.incrementSalesCount(productId, quantity, salesDate)
            } returns Unit

            // when
            topSellingProductInteractor.recordSales(productId, quantity, salesDate)

            // then
            verify(exactly = 1) {
                productSalesRankingOutputMock.incrementSalesCount(productId, quantity, salesDate)
            }
        }
    }
}
