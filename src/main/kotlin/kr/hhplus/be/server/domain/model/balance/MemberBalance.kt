package kr.hhplus.be.server.domain.model.balance

import kr.hhplus.be.server.domain.model.member.Member

/**
 * 잔액 포인트
 */
data class MemberBalance(
    val id: Long?,
    val balance: Long,
    val member: Member,
) {
    /**
     * 잔액 차감
     */
    fun reduce(amount: Long): MemberBalance {
        if (amount <= 0) {
            throw IllegalArgumentException("차감 금액은 0원 이상이어야 합니다.")
        }

        if (balance < amount) {
            throw IllegalArgumentException("잔액이 부족합니다.")
        }

        return copy(
            id = id,
            balance = balance - amount,
            member = member,
        )
    }

    /**
     * 잔액 충전
     */
    fun recharge(amount: Long): MemberBalance {
        if (amount <= 0) {
            throw IllegalArgumentException("충전 금액은 0원 이상이어야 합니다.")
        }

        return copy(
            id = id,
            balance = balance + amount,
            member = member,
        )
    }
}
