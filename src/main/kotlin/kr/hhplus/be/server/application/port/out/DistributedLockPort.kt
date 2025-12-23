package kr.hhplus.be.server.application.port.out

import java.time.Duration

/**
 * 분산 락을 획득하고 해제하는 포트.
 */
interface DistributedLockPort {
    /**
     * 분산 락을 획득하고 작업을 실행한다.
     * Exponential Backoff 재시도 전략을 사용한다.
     *
     * @param key 락 키
     * @param leaseTime 락 유지 시간
     * @param retryDelays 재시도 간격 목록 (예: [200ms, 1000ms, 2000ms])
     * @param action 락 획득 후 실행할 작업
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    fun <T> executeWithLock(
        key: String,
        leaseTime: Duration,
        retryDelays: List<Duration>,
        action: () -> T,
    ): T

    /**
     * 여러 분산 락을 동시에 획득하고 작업을 실행한다.
     * 모든 락을 획득해야 작업이 실행되며, 일부만 획득 시 모두 해제한다.
     *
     * @param keys 락 키 목록
     * @param leaseTime 락 유지 시간
     * @param retryDelays 재시도 간격 목록
     * @param action 락 획득 후 실행할 작업
     * @return 작업 실행 결과
     * @throws LockAcquisitionException 락 획득 실패 시
     */
    fun <T> executeWithMultiLock(
        keys: List<String>,
        leaseTime: Duration,
        retryDelays: List<Duration>,
        action: () -> T,
    ): T
}

/**
 * 분산 락 획득 실패 예외.
 */
class LockAcquisitionException(
    message: String,
    val retryAfterSeconds: Int = 5,
) : RuntimeException(message)
