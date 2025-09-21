package kr.hhplus.be.server.infrastructure.persistence.coupon

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CouponOwnerJpaRepository : JpaRepository<CouponOwnerJpaEntity, Long> {
    fun findByIdAndMemberId(
        id: Long,
        memberId: Long,
    ): Optional<CouponOwnerJpaEntity>

    fun findAllByMemberId(memberId: Long): List<CouponOwnerJpaEntity>
}
