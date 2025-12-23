package kr.hhplus.be.server.application.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.event.ProductCacheEvictEvent
import kr.hhplus.be.server.infrastructure.config.RedisCacheConfig
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 상품 관련 캐시 무효화 이벤트 리스너.
 * 트랜잭션이 성공적으로 커밋된 후에 캐시를 무효화한다.
 */
@Component
class ProductCacheEvictEventListener(
    private val cacheManager: CacheManager,
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleProductCacheEvict(event: ProductCacheEvictEvent) {
        logger.debug { "상품 캐시 무효화: ${event.reason}" }
        listOf(
            RedisCacheConfig.CACHE_PRODUCTS,
            RedisCacheConfig.CACHE_TOP_SELLING_PRODUCTS,
        ).forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }
}
