package kr.hhplus.be.server.domain.coupon

import kr.hhplus.be.server.domain.model.coupon.policy.FirstComeFirstServedPolicy
import kr.hhplus.be.server.domain.model.coupon.policy.NoConditionPolicy
import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

class CouponIssuanceTest {
    @Nested
    @DisplayName("쿠폰 발급")
    inner class TryIssueTest {
        // 명시적 시간 상수 정의
        private val baseNow: LocalDateTime = LocalDateTime.of(2025, 1, 10, 12, 0, 0)
        private val before1Hour: LocalDateTime = LocalDateTime.of(2025, 1, 10, 11, 0, 0)
        private val after1Hour: LocalDateTime = LocalDateTime.of(2025, 1, 10, 13, 0, 0)
        private val expiredAt: LocalDateTime = LocalDateTime.of(2025, 2, 9, 12, 0, 0)

        @Test
        @DisplayName("발급 성공 시 issuedCount 가 1 증가한다.")
        fun issueCouponSuccessIncrementsCount() {
            // given
            val member = StubFactory.member(id = 1L)
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 6L, expiredAt = expiredAt),
                    issuedCount = 3,
                    maxCount = 10,
                    startAt = before1Hour,
                    endAt = after1Hour,
                    policy = NoConditionPolicy(),
                )

            // when
            val result = sut.tryIssue(member, baseNow)

            // then
            assertThat(result.issuedCount).isEqualTo(4)
            assertThat(sut.issuedCount).isEqualTo(3)
        }

        @Test
        @DisplayName("무제한(maxCount=null) 쿠폰은 수량 소진 검사 없이 발급된다.")
        fun issueCouponUnlimitedSuccess() {
            // given
            val member = StubFactory.member(id = 2L)
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 7L, expiredAt = expiredAt),
                    issuedCount = 100,
                    maxCount = null,
                    startAt = before1Hour,
                    endAt = after1Hour,
                    policy = NoConditionPolicy(),
                )

            // when
            val result = sut.tryIssue(member, baseNow)

            // then
            assertThat(result.issuedCount).isEqualTo(101)
        }
    }

    @Nested
    @DisplayName("사용기간 검사")
    inner class CouponIsActiveTest {
        private val start: LocalDateTime = LocalDateTime.of(2025, 1, 10, 11, 0, 0)
        private val end: LocalDateTime = LocalDateTime.of(2025, 2, 10, 11, 0, 0)
        private val expiredAt: LocalDateTime = LocalDateTime.of(2025, 2, 9, 12, 0, 0)

        @Test
        @DisplayName("사용기간 이전에는 활성화되지않는다.")
        fun cannotIssueBeforeStart() {
            // given
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 1L, expiredAt = expiredAt),
                    policy = NoConditionPolicy(),
                    startAt = start,
                    endAt = end,
                )

            // when & then
            val result = sut.isActive(start.minusSeconds(1))
            assertThat(result).isFalse
        }

        @ParameterizedTest
        @ValueSource(strings = ["2025-01-10T11:00:00", "2025-02-10T11:00:00"])
        @DisplayName("사용기간에는 활성화된다")
        fun canIssueBeforeStart(baseDateTimeStr: String) {
            // given
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 2L, expiredAt = expiredAt),
                    policy = NoConditionPolicy(),
                    startAt = start,
                    endAt = end,
                )

            // when
            val result = sut.isActive(LocalDateTime.parse(baseDateTimeStr))

            // then
            assertThat(result).isTrue
        }

        @Test
        @DisplayName("사용기간이 지나면 비활성화된다.")
        fun canIssueBeforeEnd() {
            // given
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 3L, expiredAt = expiredAt),
                    policy = NoConditionPolicy(),
                    startAt = start,
                    endAt = end,
                )

            // when
            val result = sut.isActive(end.plusSeconds(1))

            // then
            assertThat(result).isFalse
        }
    }

    @Nested
    @DisplayName("발급수량 검사")
    inner class CouponHasRemainingQuotaTest {
        @Test
        @DisplayName("발급수량이 남아있으면 발급 가능하다.")
        fun canIssueWhenAvailable() {
            // given
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 1L),
                    issuedCount = 5,
                    maxCount = 10,
                    policy = NoConditionPolicy(),
                )

            // when
            val result = sut.hasRemainingQuota()

            // then
            assertThat(result).isTrue
        }

        @Test
        @DisplayName("발급수량이 모두 소진되면 발급 불가능하다.")
        fun cannotIssueWhenSoldOut() {
            // given
            val sut =
                StubFactory.couponIssuance(
                    couponSummary = StubFactory.couponSummary(id = 2L),
                    issuedCount = 10,
                    maxCount = 10,
                    policy = NoConditionPolicy(),
                )

            // when
            val result = sut.hasRemainingQuota()

            // then
            assertThat(result).isFalse
        }
    }

    @Nested
    @DisplayName("무조건 발급 정책")
    inner class NoConditionPolicyTest {
        private val baseNow: LocalDateTime = LocalDateTime.of(2025, 1, 10, 12, 0, 0)
        private val start: LocalDateTime = LocalDateTime.of(2025, 1, 10, 11, 0, 0)
        private val end: LocalDateTime = LocalDateTime.of(2025, 1, 10, 13, 0, 0)

        @Test
        @DisplayName("동일 회원이 여러 번 발급 시도해도 모두 증가한다.")
        fun noConditionPolicyAllowsMultipleIssuesForSameMember() {
            // given
            val member = StubFactory.member(id = 1L)
            val couponSummary = StubFactory.couponSummary(id = 1L)
            val policy = NoConditionPolicy()
            val issuance1 =
                StubFactory.couponIssuance(policy = policy, couponSummary = couponSummary, startAt = start, endAt = end)

            // when
            val issuance2 = issuance1.tryIssue(member = member, now = baseNow)
            val issuance3 = issuance2.tryIssue(member = member, now = baseNow.plusMinutes(1))

            // then
            assertThat(issuance2.issuedCount).isEqualTo(1)
            assertThat(issuance3.issuedCount).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("선착순 발급 정책")
    inner class FirstComeFirstServedPolicyTest {
        private val baseNow: LocalDateTime = LocalDateTime.of(2025, 1, 10, 12, 0, 0)
        private val start: LocalDateTime = LocalDateTime.of(2025, 1, 10, 11, 0, 0)
        private val end: LocalDateTime = LocalDateTime.of(2025, 1, 10, 13, 0, 0)
        private val expiredAt: LocalDateTime = LocalDateTime.of(2026, 1, 10, 13, 0, 0)

        @Disabled("CouponIssuancePolicy에서 유저의 티켓 발급여부를 확인할수있게 변경 후 테스트 작성 필요")
        @Test
        @DisplayName("이미 소유한 회원은 재발급 불가")
        fun firstComeFirstServedPolicyRejectsAlreadyOwnedMember() {
            // given
            val member = StubFactory.member(id = 1L)
            val couponSummary = StubFactory.couponSummary(id = 1L, expiredAt = expiredAt)
            val policy = FirstComeFirstServedPolicy()
            val issuance =
                StubFactory.couponIssuance(
                    couponSummary = couponSummary,
                    policy = policy,
                    startAt = start,
                    endAt = end,
                )

            // when & then
            val ex = assertThrows<IllegalStateException> { issuance.tryIssue(member, baseNow) }
            assertThat(ex.message).contains("이미 쿠폰을 발급받았습니다.")
        }

        @Disabled("CouponIssuancePolicy에서 유저의 티켓 발급여부를 확인할수있게 변경 후 테스트 작성 필요")
        @Test
        @DisplayName("소유하지 않은 회원은 발급 성공")
        fun firstComeFirstServedPolicyAllowsNewMember() {
            // given
            val member = StubFactory.member(id = 2L)
            val summaryNoOwner = StubFactory.couponSummary(id = 2L, expiredAt = expiredAt)
            val policy = FirstComeFirstServedPolicy()
            val issuance =
                StubFactory.couponIssuance(
                    couponSummary = summaryNoOwner,
                    policy = policy,
                    startAt = start,
                    endAt = end,
                )

            // when
            val result = issuance.tryIssue(member, baseNow)

            // then
            assertThat(result.issuedCount).isEqualTo(1)
        }
    }
}
