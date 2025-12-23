package kr.hhplus.be.server.infrastructure.lock

import java.util.concurrent.TimeUnit

/**
 * 분산 락을 적용하기 위한 어노테이션.
 *
 * 메서드에 적용하면 해당 메서드 실행 전 분산 락을 획득하고,
 * 실행 완료 후 락을 해제한다.
 *
 * @param key 락 키 (SpEL 표현식 지원, 예: "#memberId", "#request.id")
 * @param keyPrefix 락 키 접두사 (기본값: "lock:")
 * @param leaseTime 락 유지 시간 (기본값: 30)
 * @param timeUnit 시간 단위 (기본값: SECONDS)
 * @param retryCount 재시도 횟수 (기본값: 3)
 * @param retryDelayMillis 재시도 간격 (밀리초, 기본값: [200, 1000, 2000])
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val keyPrefix: String = "lock:",
    val leaseTime: Long = 30,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
    val retryCount: Int = 3,
    val retryDelayMillis: LongArray = [200, 1000, 2000],
)
