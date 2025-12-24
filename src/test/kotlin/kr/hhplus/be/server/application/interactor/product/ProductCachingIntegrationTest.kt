package kr.hhplus.be.server.application.interactor.product

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kr.hhplus.be.server.application.event.ProductCacheEvictEvent
import kr.hhplus.be.server.application.usecase.product.ListingProductUseCase
import kr.hhplus.be.server.common.annotation.IntegrationTest
import kr.hhplus.be.server.common.config.TestDataSourceConfig
import kr.hhplus.be.server.common.config.TestDockerComposeContainer
import kr.hhplus.be.server.infrastructure.config.RedisCacheConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant

@IntegrationTest
@SpringBootTest
@Import(
    TestDockerComposeContainer::class,
    TestDataSourceConfig::class,
)
@SqlGroup(
    Sql(scripts = ["/sql/listing-product-setup.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    Sql(scripts = ["/sql/listing-product-cleanup.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD),
)
class ProductCachingIntegrationTest {
    companion object {
        private const val RANKING_KEY = "product:sales:ranking"
    }

    @Autowired
    private lateinit var listingProductUseCase: ListingProductUseCase

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @BeforeEach
    fun setup() {
        val fixedInstant = Instant.parse("2025-09-19T15:00:00Z")
        val fixedClock = Clock.fixed(fixedInstant, Clock.systemDefaultZone().zone)

        mockkStatic(Clock::class)
        every { Clock.systemDefaultZone() } returns fixedClock

        // Redis의 모든 캐시 키를 직접 삭제하여 이전 테스트의 잔여 데이터 제거
        flushRedisCache()
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Clock::class)
        flushRedisCache()
    }

    private fun flushRedisCache() {
        val cacheNames = listOf(
            RedisCacheConfig.CACHE_PRODUCTS,
            RedisCacheConfig.CACHE_TOP_SELLING_PRODUCTS,
        )

        cacheNames.forEach { cacheName ->
            val pattern = "${cacheName}::*"
            val keys = stringRedisTemplate.keys(pattern)
            if (keys.isNotEmpty()) {
                stringRedisTemplate.delete(keys)
            }
            cacheManager.getCache(cacheName)?.clear()
        }

        // 랭킹 데이터도 초기화
        stringRedisTemplate.delete(RANKING_KEY)
    }

    @Nested
    @DisplayName("상품 목록 캐싱")
    inner class ProductListCaching {
        @Test
        @DisplayName("동일 파라미터로 조회 시 캐시된 결과를 반환한다")
        fun listingBy_cachesResult() {
            // given
            val page = 0
            val size = 10
            val sortBy = "REGISTER"
            val descending = "DESC"

            // when - 첫 번째 호출 (캐시 미스)
            val firstResult = listingProductUseCase.listingBy(page, size, sortBy, descending)

            // when - 두 번째 호출 (캐시 히트)
            val secondResult = listingProductUseCase.listingBy(page, size, sortBy, descending)

            // then - 동일한 결과 반환
            assertThat(firstResult).isEqualTo(secondResult)
            assertThat(firstResult.products).hasSize(10)

            // then - 캐시에 데이터가 존재하는지 확인
            val cache = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)
            val cacheKey = "${page}_${size}_${sortBy}_${descending}"
            assertThat(cache?.get(cacheKey)).isNotNull
        }

        @Test
        @DisplayName("다른 파라미터로 조회 시 새로운 캐시 엔트리가 생성된다")
        fun listingBy_differentParamsCreateDifferentCache() {
            // given
            val page = 0
            val size = 10
            val sortBy = "REGISTER"

            // when - 다른 정렬 순서로 조회
            val descResult = listingProductUseCase.listingBy(page, size, sortBy, "DESC")
            val ascResult = listingProductUseCase.listingBy(page, size, sortBy, "ASC")

            // then - 다른 결과 반환 (정렬 순서가 다름)
            assertThat(descResult.products.first().id).isNotEqualTo(ascResult.products.first().id)

            // then - 각각 캐시에 저장됨
            val cache = cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)
            assertThat(cache?.get("${page}_${size}_${sortBy}_DESC")).isNotNull
            assertThat(cache?.get("${page}_${size}_${sortBy}_ASC")).isNotNull
        }
    }

    @Nested
    @DisplayName("캐시 무효화")
    inner class CacheEviction {
        @Test
        @DisplayName("ProductCacheEvictEvent 발행 시 상품 목록 캐시가 무효화된다")
        fun productCacheEvictEvent_clearsProductListCache() {
            // given - 캐시를 먼저 채움
            listingProductUseCase.listingBy(0, 10, "REGISTER", "DESC")

            // then - 캐시에 데이터가 존재
            assertThat(cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)?.get("0_10_REGISTER_DESC")).isNotNull

            // when - 트랜잭션 내에서 이벤트 발행
            transactionTemplate.execute {
                eventPublisher.publishEvent(ProductCacheEvictEvent(reason = "테스트 캐시 무효화"))
            }

            // then - 상품 목록 캐시가 무효화됨
            assertThat(cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)?.get("0_10_REGISTER_DESC")).isNull()
        }

        @Test
        @DisplayName("트랜잭션 롤백 시 캐시가 무효화되지 않는다")
        fun transactionRollback_doesNotEvictCache() {
            // given - 캐시를 먼저 채움
            listingProductUseCase.listingBy(0, 10, "REGISTER", "DESC")

            assertThat(cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)?.get("0_10_REGISTER_DESC")).isNotNull

            // when - 트랜잭션 롤백
            try {
                transactionTemplate.execute {
                    eventPublisher.publishEvent(ProductCacheEvictEvent(reason = "롤백될 작업"))
                    throw RuntimeException("의도적 롤백")
                }
            } catch (e: RuntimeException) {
                // 예외 무시
            }

            // then - 캐시가 유지됨 (AFTER_COMMIT이므로 롤백 시 이벤트 리스너 미실행)
            assertThat(cacheManager.getCache(RedisCacheConfig.CACHE_PRODUCTS)?.get("0_10_REGISTER_DESC")).isNotNull
        }
    }
}
