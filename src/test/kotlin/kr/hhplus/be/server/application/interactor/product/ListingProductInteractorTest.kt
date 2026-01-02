package kr.hhplus.be.server.application.interactor.product

import io.mockk.every
import io.mockk.mockk
import kr.hhplus.be.server.application.enums.ListingProductDescending
import kr.hhplus.be.server.application.enums.ListingProductSortBy
import kr.hhplus.be.server.application.port.out.ListingProductOutput
import kr.hhplus.be.server.application.vo.ProductSummaryItemVO
import kr.hhplus.be.server.common.annotation.UnitTest
import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@UnitTest
class ListingProductInteractorTest {
    @Nested
    @DisplayName("상품 목록 조회")
    inner class ListingByTest {
        private val listingProductOutputMock = mockk<ListingProductOutput>()
        private val listingProductInteractor =
            ListingProductInteractor(
                listingProductOutput = listingProductOutputMock,
            )

        @Test
        @DisplayName("상품 목록이 정상적으로 반환된다")
        fun listingBy() {
            // given
            val page = 0
            val size = 2
            val sortBy = "REGISTER"
            val descending = "DESC"
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
                listingProductOutputMock.listingBy(
                    page = page,
                    size = size,
                    sortBy = ListingProductSortBy.REGISTER,
                    descending = ListingProductDescending.DESC,
                )
            } returns productSummaries

            // when
            val result = listingProductInteractor.listingBy(page, size, sortBy, descending)

            // then
            assertThat(result.rows).isEqualTo(2)
            assertThat(result.page).isEqualTo(page)
            assertThat(result.products).containsExactly(
                ProductSummaryItemVO(
                    id = 1L,
                    name = "Apple iPhone 15 Pro",
                    price = 1700000,
                    stockQuantity = 5,
                ),
                ProductSummaryItemVO(
                    id = 2L,
                    name = "Samsung Galaxy S24 Ultra",
                    price = 1600000,
                    stockQuantity = 3,
                ),
            )
        }

        @Test
        @DisplayName("지원하지 않는 정렬 기준이면 예외가 발생한다")
        fun listingBy_notSupportedSortBy() {
            // given
            val page = 0
            val size = 2
            val sortBy = "NOT_EXIST"
            val descending = "DESC"

            // when & then
            assertThatThrownBy {
                listingProductInteractor.listingBy(page, size, sortBy, descending)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("지원하지않는 정렬기준입니다")
        }

        @Test
        @DisplayName("지원하지 않는 정렬 차순이면 예외가 발생한다")
        fun listingBy_notSupportedDescending() {
            // given
            val page = 0
            val size = 2
            val sortBy = "REGISTER"
            val descending = "NOT_EXIST"

            // when & then
            assertThatThrownBy {
                listingProductInteractor.listingBy(page, size, sortBy, descending)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("지원하지않는 정렬차순입니다")
        }
    }
}
