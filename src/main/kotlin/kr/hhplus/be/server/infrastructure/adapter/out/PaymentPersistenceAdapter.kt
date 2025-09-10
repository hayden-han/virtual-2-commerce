package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.PaymentOutput
import kr.hhplus.be.server.domain.model.payment.PaymentSummary
import kr.hhplus.be.server.infrastructure.persistence.payment.PaymentSummaryJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.payment.PaymentSummaryJpaRepository
import org.springframework.stereotype.Component

@Component
class PaymentPersistenceAdapter(
    private val paymentSummaryJpaRepository: PaymentSummaryJpaRepository,
) : PaymentOutput {
    override fun save(paymentSummary: PaymentSummary): PaymentSummary {
        val entity: PaymentSummaryJpaEntity = PaymentSummaryJpaEntity.from(paymentSummary)
        return paymentSummaryJpaRepository.save(entity).toDomain()
    }
}
