package kr.hhplus.be.server.application.interactor.balance

import kr.hhplus.be.server.application.port.out.MyBalanceOutput
import kr.hhplus.be.server.domain.model.balance.MemberBalance
import kr.hhplus.be.server.domain.utils.StubFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

class MyBalanceInteractorTest {
    @Nested
    @DisplayName("잔액 조회")
    inner class GetMyBalance {
        private val myBalanceOutput = mock<MyBalanceOutput>()
        private val myBalanceInteractor = MyBalanceInteractor(myBalanceOutput = myBalanceOutput)

        @Test
        @DisplayName("존재하지 않는 회원의 잔액을 조회하는 경우 예외가 발생한다")
        fun getMyBalance_NotExistMember() {
            // given
            val memberId = 1L
            `when`(
                myBalanceOutput.findByMemberId(1L),
            ).thenReturn(Optional.empty())

            val exception =
                assertThrows<IllegalArgumentException> {
                    myBalanceInteractor.getMyBalance(memberId)
                }
            assertThat(exception.message).isEqualTo("회원의 잔고를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("존재하는 회원의 잔액을 조회하는 경우 잔액이 반환된다")
        fun getMyBalance_ExistMember() {
            // given
            val member = StubFactory.member(id = 2L)
            val memberBalance =
                MemberBalance(
                    id = 1L,
                    member = member,
                    balance = 1000L,
                )
            `when`(
                myBalanceOutput.findByMemberId(2L),
            ).thenReturn(Optional.of(memberBalance))

            // when
            val actualBalance = myBalanceInteractor.getMyBalance(2L)

            // then
            assertThat(actualBalance.member.id).isEqualTo(2L)
            assertThat(actualBalance.balance).isEqualTo(1000L)
        }
    }

    @Test
    fun rechargeMyBalance() {
    }

    @Test
    fun reduceMyBalance() {
    }
}
