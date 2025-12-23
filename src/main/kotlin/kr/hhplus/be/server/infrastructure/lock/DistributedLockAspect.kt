package kr.hhplus.be.server.infrastructure.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.port.out.DistributedLockPort
import kr.hhplus.be.server.application.port.out.LockAcquisitionException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * @DistributedLock 어노테이션을 처리하는 AOP Aspect.
 *
 * SpEL 표현식을 통해 동적으로 락 키를 생성하고,
 * Exponential Backoff 재시도 전략으로 락을 획득한다.
 */
@Aspect
@Component
class DistributedLockAspect(
    private val distributedLockPort: DistributedLockPort,
) {
    private val logger = KotlinLogging.logger {}
    private val spelParser = SpelExpressionParser()
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Around("@annotation(distributedLock)")
    fun around(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock,
    ): Any? {
        val lockKey = generateLockKey(joinPoint, distributedLock)
        val leaseTime = Duration.of(distributedLock.leaseTime, distributedLock.timeUnit.toChronoUnit())
        val retryDelays = distributedLock.retryDelayMillis.map { Duration.ofMillis(it) }

        logger.debug { "분산 락 획득 시도: $lockKey" }

        return try {
            distributedLockPort.executeWithLock(
                key = lockKey,
                leaseTime = leaseTime,
                retryDelays = retryDelays,
            ) {
                joinPoint.proceed()
            }
        } catch (e: LockAcquisitionException) {
            logger.warn { "분산 락 획득 실패: $lockKey - ${e.message}" }
            throw e
        }
    }

    private fun generateLockKey(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock,
    ): String {
        val keyExpression = distributedLock.key
        val prefix = distributedLock.keyPrefix

        // SpEL 표현식이 아닌 경우 (# 시작하지 않는 경우) 그대로 사용
        if (!keyExpression.startsWith("#")) {
            return "$prefix$keyExpression"
        }

        // SpEL 표현식 파싱
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val args = joinPoint.args

        val context = MethodBasedEvaluationContext(
            joinPoint.target,
            method,
            args,
            parameterNameDiscoverer,
        )

        val expression = spelParser.parseExpression(keyExpression)
        val evaluatedKey = expression.getValue(context, String::class.java)
            ?: throw IllegalArgumentException("SpEL 표현식 평가 결과가 null입니다: $keyExpression")

        return "$prefix$evaluatedKey"
    }
}
