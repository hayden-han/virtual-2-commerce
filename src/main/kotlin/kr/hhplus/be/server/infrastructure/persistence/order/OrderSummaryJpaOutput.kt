package kr.hhplus.be.server.infrastructure.persistence.order

import kr.hhplus.be.server.application.port.out.OrderSummaryOutput
import org.springframework.data.jpa.repository.JpaRepository

interface OrderSummaryJpaOutput :
    OrderSummaryOutput,
    JpaRepository<OrderSummaryJpaEntity, Long>
