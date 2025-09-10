package kr.hhplus.be.server.infrastructure.persistence.product

import org.springframework.data.jpa.repository.JpaRepository

interface ProductSummaryJpaRepository : JpaRepository<ProductSummaryJpaEntity, Long> {
    fun findAllByIdIn(ids: Collection<Long>): List<ProductSummaryJpaEntity>
}
