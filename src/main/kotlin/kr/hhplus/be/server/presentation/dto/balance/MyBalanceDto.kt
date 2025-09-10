package kr.hhplus.be.server.presentation.dto.balance

import kr.hhplus.be.server.domain.model.balance.MemberBalance

data class MyBalanceResponse(
    val balanceId: Long,
    val availableBalance: Long,
) {
    companion object {
        fun from(memberBalance: MemberBalance): MyBalanceResponse =
            MyBalanceResponse(
                balanceId = memberBalance.id!!,
                availableBalance = memberBalance.balance,
            )
    }
}

data class RechargeMyBalanceRequest(
    val chargeAmount: Long,
)

data class RechargeMyBalanceResponse(
    val balanceId: Long,
    val availableBalance: Long,
) {
    companion object {
        fun from(rechargedBalance: MemberBalance): RechargeMyBalanceResponse =
            RechargeMyBalanceResponse(
                balanceId = rechargedBalance.id!!,
                availableBalance = rechargedBalance.balance,
            )
    }
}
