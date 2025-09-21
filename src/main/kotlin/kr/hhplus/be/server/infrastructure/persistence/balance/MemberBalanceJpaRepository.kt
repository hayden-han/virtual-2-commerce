package kr.hhplus.be.server.infrastructure.persistence.balance

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberBalanceJpaRepository : JpaRepository<MemberBalanceJpaEntity, Long> {
    fun findByMemberId(memberId: Long): Optional<MemberBalanceJpaEntity>

    fun findByIdAndMemberId(
        memberBalanceId: Long,
        memberId: Long,
    ): Optional<MemberBalanceJpaEntity>
}
