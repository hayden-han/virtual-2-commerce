package kr.hhplus.be.server.infrastructure.persistence.balance

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity
import kr.hhplus.be.server.infrastructure.persistence.member.MemberJpaEntity

@Entity
@Table(name = "member_balance")
class MemberBalanceJpaEntity(
    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    val member: MemberJpaEntity,
) : CreatedAndUpdatedAtAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var balance: Long = 0L
}
