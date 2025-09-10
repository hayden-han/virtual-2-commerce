package kr.hhplus.be.server.application.interactor.balance

import kr.hhplus.be.server.application.port.out.MyBalanceOutput
import kr.hhplus.be.server.application.usecase.balance.MyBalanceUseCase
import kr.hhplus.be.server.domain.model.balance.MemberBalance
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyBalanceInteractor(
    private val myBalanceOutput: MyBalanceOutput,
) : MyBalanceUseCase {
    @Transactional(readOnly = true)
    override fun getMyBalance(memberId: Long): MemberBalance =
        myBalanceOutput
            .findByMemberId(memberId)
            .orElseThrow { IllegalArgumentException("회원의 잔고를 찾을 수 없습니다.") }

    @Transactional
    override fun rechargeMyBalance(
        memberId: Long,
        memberBalanceId: Long,
        amount: Long,
    ): MemberBalance {
        val rechargedBalance: MemberBalance =
            myBalanceOutput
                .findByIdAndMemberId(memberBalanceId, memberId)
                .orElseThrow { IllegalArgumentException("회원의 잔고를 찾을 수 없습니다.") }
                .recharge(amount)

        return myBalanceOutput.save(rechargedBalance)
    }

    @Transactional
    override fun reduceMyBalance(
        memberId: Long,
        amount: Long,
    ): MemberBalance {
        val reducedBalance: MemberBalance =
            myBalanceOutput
                .findByMemberId(memberId)
                .orElseThrow { IllegalArgumentException("회원의 잔고를 찾을 수 없습니다.") }
                .reduce(amount)
        return myBalanceOutput.save(reducedBalance)
    }
}
