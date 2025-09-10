package kr.hhplus.be.server.infrastructure.persistence.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.member.Member

@Entity
@Table(name = "member")
class MemberJpaEntity(
    @Column(nullable = false)
    val email: String,
    @Column(nullable = false)
    val pwd: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    fun toDomain(): Member =
        Member(
            id = this.id,
            email = this.email,
            pwd = this.pwd,
        )

    companion object {
        fun from(domain: Member): MemberJpaEntity =
            MemberJpaEntity(
                email = domain.email,
                pwd = domain.pwd,
            ).apply {
                this.id = domain.id
            }
    }
}
