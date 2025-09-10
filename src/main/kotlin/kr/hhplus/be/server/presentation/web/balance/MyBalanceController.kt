package kr.hhplus.be.server.presentation.web.balance

import kr.hhplus.be.server.application.usecase.balance.MyBalanceUseCase
import kr.hhplus.be.server.presentation.dto.balance.MyBalanceResponse
import kr.hhplus.be.server.presentation.dto.balance.RechargeMyBalanceRequest
import kr.hhplus.be.server.presentation.dto.balance.RechargeMyBalanceResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("(/api/v1/balances/me")
class MyBalanceController(
    private val myBalanceUseCase: MyBalanceUseCase,
) {
    @GetMapping
    fun getMyBalance(
        @RequestHeader("X-Member-Id") memberId: Long,
    ): MyBalanceResponse =
        MyBalanceResponse.from(
            myBalanceUseCase.getMyBalance(memberId),
        )

    @PutMapping("{memberBalanceId}/recharge")
    fun rechargeMyBalance(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable memberBalanceId: Long,
        @RequestBody requestData: RechargeMyBalanceRequest,
    ): RechargeMyBalanceResponse =
        RechargeMyBalanceResponse.from(
            myBalanceUseCase
                .rechargeMyBalance(
                    memberId = memberId,
                    memberBalanceId = memberBalanceId,
                    amount = requestData.chargeAmount,
                ),
        )
}
