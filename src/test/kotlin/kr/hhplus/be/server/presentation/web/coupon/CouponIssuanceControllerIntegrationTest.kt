package kr.hhplus.be.server.presentation.web.coupon

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kr.hhplus.be.server.common.annotation.IntegrationTest
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@IntegrationTest
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(value = ["/sql/coupon-issue-sample.sql"])
@Transactional
class CouponIssuanceControllerIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val fixedNow = LocalDateTime.of(2025, 9, 24, 0, 0, 0)

    @BeforeEach
    fun setUp() {
        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returns fixedNow
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDateTime::class)
    }

    @Nested
    @DisplayName("쿠폰 발급(POST /api/v1/coupons/issuance)")
    inner class IssueCouponTest {
        @Test
        @DisplayName("정상 발급: 기간 내, 수량 미달, 정책 통과")
        fun issueCoupon_success() {
            val memberId = 1L
            val couponSummaryId = 1L // 정상쿠폰
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/coupons/issuance")
                    .header("X-Member-Id", memberId)
                    .content("{\"couponSummaryId\": $couponSummaryId}")
                    .contentType("application/json")
            ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.couponId").exists())
        }

        @Test
        @DisplayName("실패: 발급 기간이 아님")
        fun issueCoupon_fail_period() {
            val memberId = 1L
            val couponSummaryId = 2L // 기간외쿠폰
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/coupons/issuance")
                    .header("X-Member-Id", memberId)
                    .content("{\"couponSummaryId\": $couponSummaryId}")
                    .contentType("application/json")
            ).andExpect(MockMvcResultMatchers.status().isConflict)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("쿠폰발급이 가능한 기간이 아닙니다."))
        }

        @Test
        @DisplayName("실패: 수량 소진")
        fun issueCoupon_fail_maxCount() {
            val memberId = 1L
            val couponSummaryId = 3L // 수량소진쿠폰
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/coupons/issuance")
                    .header("X-Member-Id", memberId)
                    .content("{\"couponSummaryId\": $couponSummaryId}")
                    .contentType("application/json")
            ).andExpect(MockMvcResultMatchers.status().isConflict)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("쿠폰발급수량이 부족합니다."))
        }

        @Test
        @DisplayName("실패: 정책 위반(중복 발급 등)")
        fun issueCoupon_fail_policy() {
            val memberId = 2L // 이미 발급된 회원
            val couponSummaryId = 1L // 정상쿠폰
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/coupons/issuance")
                    .header("X-Member-Id", memberId)
                    .content("{\"couponSummaryId\": $couponSummaryId}")
                    .contentType("application/json")
            ).andExpect(MockMvcResultMatchers.status().isConflict)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("중복발급이 제한된 쿠폰입니다."))
        }
    }
}