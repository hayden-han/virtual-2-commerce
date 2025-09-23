package kr.hhplus.be.server.infrastructure.persistence.coupon

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface couponJpaRepository : JpaRepository<CouponJpaEntity, Long> {
    fun findByIdAndMemberId(
        id: Long,
        memberId: Long,
    ): Optional<CouponJpaEntity>

    fun findAllByMemberId(memberId: Long): List<CouponJpaEntity>
}
