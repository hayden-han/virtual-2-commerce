package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.MyBalanceOutput
import kr.hhplus.be.server.domain.model.balance.MemberBalance
import kr.hhplus.be.server.infrastructure.persistence.balance.MemberBalanceJpaEntity
import kr.hhplus.be.server.infrastructure.persistence.balance.MemberBalanceJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Component
class MyBalancePersistenceAdapter(
    private val repository: MemberBalanceJpaRepository,
) : MyBalanceOutput {
    @Transactional(readOnly = true)
    override fun findByMemberId(memberId: Long): Optional<MemberBalance> =
        repository
            .findByMemberId(memberId)
            .map(MemberBalanceJpaEntity::toDomain)

    override fun findByIdAndMemberId(
        memberBalanceId: Long,
        memberId: Long,
    ): Optional<MemberBalance> {
        TODO("Not yet implemented")
    }

    override fun save(myBalance: MemberBalance): MemberBalance {
        TODO("Not yet implemented")
    }
}
