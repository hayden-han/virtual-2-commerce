package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.MemberOutput
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.infrastructure.persistence.member.MemberJpaRepository
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class MemberPersistenceAdapter(
    private val memberRepository: MemberJpaRepository,
) : MemberOutput {
    override fun findById(memberId: Long): Optional<Member> = memberRepository.findById(memberId).map { it.toDomain() }
}
