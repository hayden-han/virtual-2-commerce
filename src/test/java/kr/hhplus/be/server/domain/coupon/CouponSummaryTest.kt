package kr.hhplus.be.server.domain.coupon

import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class CouponSummaryTest {
    @Nested
    @DisplayName("쿠폰 만료 여부 확인")
    inner class IsExpiredTest {
        @Test
        @DisplayName("현재 시간이 만료일 이후인 경우 쿠폰이 만료되었다고 반환한다.")
        fun isExpiredReturnsTrueForExpiredCoupon() {
            // given
            val expiredAt = LocalDateTime.of(2024, 1, 1, 0, 0)
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "Test Coupon",
                    discountPercentage = 10L,
                    expiredAt = expiredAt,
                )
            val now = LocalDateTime.of(2024, 1, 2, 0, 0)

            // when
            val result = couponSummary.isExpired(now)

            // then
            assertThat(result).isTrue
        }

        @Test
        @DisplayName("현재 시간이 만료일인 경우 쿠폰이 만료되지 않았다고 반환한다.")
        fun isExpiredReturnsFalseForValidCoupon() {
            // given
            val expiredAt = LocalDateTime.of(2024, 1, 1, 0, 0)
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "Test Coupon",
                    discountPercentage = 10L,
                    expiredAt = expiredAt,
                )
            val now = LocalDateTime.of(2024, 1, 1, 0, 0)

            // when
            val result = couponSummary.isExpired(now)

            // then
            assertThat(result).isFalse
        }
    }
}
