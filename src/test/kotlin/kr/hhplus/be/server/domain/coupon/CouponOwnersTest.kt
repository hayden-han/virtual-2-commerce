package kr.hhplus.be.server.domain.coupon

import kr.hhplus.be.server.common.annotation.UnitTest
import kr.hhplus.be.server.domain.exception.ConflictResourceException
import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@UnitTest
class CouponOwnersTest {
    @Nested
    @DisplayName("할인금액 계산")
    inner class CalculateDiscountedPriceTest {
        @Test
        @DisplayName("할인율이 25%인 쿠폰으로 금액 4000원에 적용하면 1000원이 할인된다.")
        fun calculateDiscountedPriceApplies25PercentDiscount() {
            // given
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "[블랙프라이데이] 25% 할인 쿠폰",
                    discountPercentage = 25L,
                    expiredAt = LocalDateTime.of(2024, 11, 29, 0, 0),
                )
            val couponOwners =
                StubFactory.couponOwner(
                    id = 1L,
                    couponSummary = couponSummary,
                    memberId = 1L,
                    usingAt = null,
                )

            // when
            val discountedPrice = couponOwners.calculateDiscountAmount(4000L)

            // then
            assertThat(discountedPrice).isEqualTo(1000L)
        }
    }

    @Nested
    @DisplayName("쿠폰 사용")
    inner class UseTest {
        @Test
        @DisplayName("유효기간이 지난 쿠폰을 사용하면 예외가 발생한다.")
        fun useExpiredCouponThrowsException() {
            // given
            val member = StubFactory.member(id = 1L)
            val expiredAt = LocalDateTime.of(2024, 12, 25, 0, 0)
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "[크리스마스이브] 10% 할인 쿠폰",
                    discountPercentage = 10L,
                    expiredAt = expiredAt,
                )
            val couponOwners =
                StubFactory.couponOwner(
                    id = 1L,
                    couponSummary = couponSummary,
                    memberId = 1L,
                    usingAt = null,
                )

            // when & then
            val exception =
                assertThrows<ConflictResourceException> {
                    couponOwners.using(
                        member = member,
                        now = LocalDateTime.of(2024, 12, 25, 0, 0, 1),
                    )
                }
            assertThat(exception.message).isEqualTo("만료된 쿠폰입니다.")
        }

        @Test
        @DisplayName("이미 사용된 쿠폰을 사용하면 예외가 발생한다.")
        fun useAlreadyUsedCouponThrowsException() {
            // given
            val member = StubFactory.member(id = 2L)
            val expiredAt = LocalDateTime.of(2024, 12, 25, 0, 0)
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "[크리스마스이브] 10% 할인 쿠폰",
                    discountPercentage = 10L,
                    expiredAt = expiredAt,
                )
            val couponOwners =
                StubFactory.couponOwner(
                    id = 1L,
                    couponSummary = couponSummary,
                    memberId = 2L,
                    usingAt = LocalDateTime.of(2024, 12, 24, 12, 0, 0),
                )

            // when & then
            val exception =
                assertThrows<ConflictResourceException> {
                    couponOwners.using(
                        member = member,
                        now = LocalDateTime.of(2024, 12, 24, 12, 0, 1),
                    )
                }
            assertThat(exception.message).isEqualTo("이미 사용된 쿠폰입니다.")
        }

        @Test
        @DisplayName("유효한 쿠폰을 사용하면 사용 시간이 기록된다.")
        fun useValidCouponRecordsUsingAt() {
            // given
            val member = StubFactory.member(id = 3L)
            val now = LocalDateTime.of(2024, 12, 24, 23, 59, 59)
            val expiredAt = LocalDateTime.of(2024, 12, 25, 0, 0)
            val couponSummary =
                StubFactory.couponSummary(
                    id = 1L,
                    name = "[크리스마스이브] 10% 할인 쿠폰",
                    discountPercentage = 10L,
                    expiredAt = expiredAt,
                )
            val couponOwners =
                StubFactory.couponOwner(
                    id = 1L,
                    couponSummary = couponSummary,
                    memberId = 3L,
                    usingAt = null,
                )

            // when
            val usedCoupon =
                couponOwners.using(
                    member = member,
                    now = now,
                )

            // then
            assertThat(usedCoupon.usingAt).isEqualTo(now)
        }
    }
}
