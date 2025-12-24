package kr.hhplus.be.server.presentation.web.product

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kr.hhplus.be.server.common.annotation.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import java.time.Clock
import java.time.Instant

@IntegrationTest
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@SqlGroup(
    Sql(scripts = ["/sql/listing-product-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/listing-product-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class ListingProductControllerIntegrationTest {
    companion object {
        private const val RANKING_KEY_PREFIX = "product:sales:ranking"
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @BeforeEach
    fun setup() {
        val fixedInstant = Instant.parse("2025-09-19T15:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, Clock.systemDefaultZone().zone)

        mockkStatic(Clock::class)
        every { Clock.systemDefaultZone() } returns fixedClock

        // Redis 랭킹 데이터 초기화
        flushRankingData()
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Clock::class)
        flushRankingData()
    }

    private fun flushRankingData() {
        val keys = stringRedisTemplate.keys("$RANKING_KEY_PREFIX:*")
        if (keys.isNotEmpty()) {
            stringRedisTemplate.delete(keys)
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 API [GET /api/v1/products]")
    inner class ListingProducts {
        @Test
        @DisplayName("상품 목록을 조회한다.")
        fun listingProductsTest() {
            // given
            val memberId = 1L

            // when & then
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/products")
                        .header("X-Member-Id", memberId)
                        .queryParams(
                            LinkedMultiValueMap(
                                mapOf(
                                    "page" to listOf("0"),
                                    "size" to listOf("10"),
                                ),
                            ),
                        ),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.rows").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.page").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("$.products.length()").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].id").value(3L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[1].id").value(7L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[2].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[3].id").value(8L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[4].id").value(5L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[5].id").value(10L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[6].id").value(4L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[7].id").value(6L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[8].id").value(9L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[9].id").value(2L))
        }
    }

    @Nested
    @DisplayName("최근 n일간 가장 많이 팔린 상위 m개 상품 조회 API [GET /api/v1/products/top-selling]")
    inner class ListingTopSellingProductTest {
        @Test
        @DisplayName("최근 3일간 가장 많이 팔린 상위 5개 상품을 조회한다.")
        fun listingTopSellingProductsTest() {
            // given
            val nDay = 3
            val mProduct = 5

            // Redis에 날짜별 판매 랭킹 데이터 설정 (nDay=3, curDate=2025-09-19이므로 9/17, 9/18, 9/19)
            // 9/17: 상품4=2, 상품3=1
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-17", "4", 2.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-17", "3", 1.0)
            // 9/18: 상품4=1, 상품10=2, 상품1=1
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-18", "4", 1.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-18", "10", 2.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-18", "1", 1.0)
            // 9/19: 상품4=1, 상품3=2, 상품10=1, 상품1=1, 상품9=1
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-19", "4", 1.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-19", "3", 2.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-19", "10", 1.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-19", "1", 1.0)
            stringRedisTemplate.opsForZSet().add("$RANKING_KEY_PREFIX:2025-09-19", "9", 1.0)
            // 합산: 상품4=4, 상품3=3, 상품10=3, 상품1=2, 상품9=1

            // when & then
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/products/top-selling")
                        .queryParams(
                            LinkedMultiValueMap(
                                mapOf(
                                    "nDay" to listOf(nDay.toString()),
                                    "mProduct" to listOf(mProduct.toString()),
                                ),
                            ),
                        ),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products").isArray)
                .andExpect(MockMvcResultMatchers.jsonPath("$.products.length()").value(5))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].id").value(4L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[0].totalOrderQuantity").value(4))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[1].id").value(3L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[1].totalOrderQuantity").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[2].id").value(10L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[2].totalOrderQuantity").value(3))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[3].id").value(1L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[3].totalOrderQuantity").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[4].id").value(9L))
                .andExpect(MockMvcResultMatchers.jsonPath("$.products[4].totalOrderQuantity").value(1))
        }

        @ParameterizedTest
        @DisplayName("n이 0 이하인 경우 예외가 발생한다.")
        @CsvSource(
            "0, 5",
            "-1, 5",
        )
        fun listingTopSellingProducts_invalidNDay_exception(
            nDay: Int,
            mProduct: Int,
        ) {
            // when & then
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/products/top-selling")
                        .queryParams(
                            LinkedMultiValueMap(
                                mapOf(
                                    "nDay" to listOf(nDay.toString()),
                                    "mProduct" to listOf(mProduct.toString()),
                                ),
                            ),
                        ),
                ).andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("조회 기간은 0보다 커야합니다. (nDay: $nDay)"))
        }

        @ParameterizedTest
        @DisplayName("m이 0 이하인 경우 예외가 발생한다.")
        @CsvSource(
            "3, 0",
            "3, -1",
        )
        fun listingTopSellingProducts_invalidLimit_exception(
            nDay: Int,
            mProduct: Int,
        ) {
            // when & then
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/products/top-selling")
                        .queryParams(
                            LinkedMultiValueMap(
                                mapOf(
                                    "nDay" to listOf(nDay.toString()),
                                    "mProduct" to listOf(mProduct.toString()),
                                ),
                            ),
                        ),
                ).andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("조회 갯수는 0보다 커야합니다. (limit: $mProduct)"))
        }

        @ParameterizedTest
        @DisplayName("n과 m이 모두 0 이하인 경우 n에 대한 예외가 먼저 발생한다.")
        @CsvSource(
            "0, 0",
            "-1, -1",
        )
        fun listingTopSellingProducts_bothInvalid_exception(
            nDay: Int,
            mProduct: Int,
        ) {
            // when & then
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/products/top-selling")
                        .queryParams(
                            LinkedMultiValueMap(
                                mapOf(
                                    "nDay" to listOf(nDay.toString()),
                                    "mProduct" to listOf(mProduct.toString()),
                                ),
                            ),
                        ),
                ).andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("조회 기간은 0보다 커야합니다. (nDay: $nDay)"))
        }
    }
}
