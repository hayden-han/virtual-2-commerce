package kr.hhplus.be.server.application.event

/**
 * 상품 관련 캐시 무효화를 트리거하는 이벤트.
 * 트랜잭션 커밋 후에 캐시를 무효화하기 위해 사용된다.
 *
 * @param reason 캐시 무효화 사유
 */
data class ProductCacheEvictEvent(
    val reason: String,
)
