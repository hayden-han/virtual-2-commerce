package kr.hhplus.be.server.infrastructure.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.port.out.DistributedLockPort
import kr.hhplus.be.server.application.port.out.LockAcquisitionException
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RedissonDistributedLockAdapter(
    private val redissonClient: RedissonClient,
) : DistributedLockPort {
    private val logger = KotlinLogging.logger {}

    override fun <T> executeWithLock(
        key: String,
        leaseTime: Duration,
        retryDelays: List<Duration>,
        action: () -> T,
    ): T {
        val lock = redissonClient.getLock(key)
        val totalRetries = retryDelays.size + 1 // 첫 번째 시도 포함

        // 첫 번째 시도 (즉시)
        if (lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
            logger.debug { "락 획득 성공 (1차 시도): $key" }
            return executeAndUnlock(lock, key, action)
        }

        // 재시도 (Exponential Backoff)
        retryDelays.forEachIndexed { index, delay ->
            val attemptNumber = index + 2 // 2차, 3차, 4차...
            logger.debug { "락 획득 대기 중 ($attemptNumber/$totalRetries): $key, ${delay.toMillis()}ms 후 재시도" }

            Thread.sleep(delay.toMillis())

            if (lock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.debug { "락 획득 성공 (${attemptNumber}차 시도): $key" }
                return executeAndUnlock(lock, key, action)
            }
        }

        // 모든 재시도 실패
        logger.warn { "락 획득 실패 (최대 재시도 초과): $key" }
        throw LockAcquisitionException(
            message = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            retryAfterSeconds = 5,
        )
    }

    override fun <T> executeWithMultiLock(
        keys: List<String>,
        leaseTime: Duration,
        retryDelays: List<Duration>,
        action: () -> T,
    ): T {
        if (keys.isEmpty()) {
            return action()
        }

        if (keys.size == 1) {
            return executeWithLock(keys.first(), leaseTime, retryDelays, action)
        }

        // 데드락 방지를 위해 키를 정렬하여 일관된 순서로 락 획득
        val sortedKeys = keys.sorted()
        val locks = sortedKeys.map { redissonClient.getLock(it) }
        val multiLock = redissonClient.getMultiLock(*locks.toTypedArray())
        val totalRetries = retryDelays.size + 1

        // 첫 번째 시도
        if (multiLock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
            logger.debug { "멀티 락 획득 성공 (1차 시도): $sortedKeys" }
            return executeAndUnlockMulti(multiLock, sortedKeys, action)
        }

        // 재시도 (Exponential Backoff)
        retryDelays.forEachIndexed { index, delay ->
            val attemptNumber = index + 2
            logger.debug { "멀티 락 획득 대기 중 ($attemptNumber/$totalRetries): $sortedKeys, ${delay.toMillis()}ms 후 재시도" }

            Thread.sleep(delay.toMillis())

            if (multiLock.tryLock(0, leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.debug { "멀티 락 획득 성공 (${attemptNumber}차 시도): $sortedKeys" }
                return executeAndUnlockMulti(multiLock, sortedKeys, action)
            }
        }

        logger.warn { "멀티 락 획득 실패 (최대 재시도 초과): $sortedKeys" }
        throw LockAcquisitionException(
            message = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            retryAfterSeconds = 5,
        )
    }

    private fun <T> executeAndUnlock(
        lock: org.redisson.api.RLock,
        key: String,
        action: () -> T,
    ): T =
        try {
            action()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug { "락 해제: $key" }
            }
        }

    private fun <T> executeAndUnlockMulti(
        multiLock: org.redisson.api.RLock,
        keys: List<String>,
        action: () -> T,
    ): T =
        try {
            action()
        } finally {
            if (multiLock.isHeldByCurrentThread) {
                multiLock.unlock()
                logger.debug { "멀티 락 해제: $keys" }
            }
        }
}
