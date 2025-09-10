package kr.hhplus.be.server.infrastructure.persistence.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Aspect
@Component
class DataSourceRoutingAspect {
    @Around(
        "@annotation(org.springframework.transaction.annotation.Transactional)" +
            " || @within(org.springframework.transaction.annotation.Transactional)",
    )
    fun routeDataSource(joinPoint: ProceedingJoinPoint): Any? {
        val method =
            joinPoint.signature.declaringType.getDeclaredMethod(
                joinPoint.signature.name,
                *joinPoint.args.map { it?.javaClass ?: Any::class.java }.toTypedArray(),
            )
        val transactional =
            AnnotationUtils.findAnnotation(method, Transactional::class.java)
                ?: AnnotationUtils.findAnnotation(joinPoint.target.javaClass, Transactional::class.java)
        val readOnly = transactional?.readOnly ?: false
        try {
            DataSourceContextHolder.set(if (readOnly) DataSourceType.SLAVE else DataSourceType.MASTER)
            return joinPoint.proceed()
        } finally {
            DataSourceContextHolder.clear()
        }
    }
}
