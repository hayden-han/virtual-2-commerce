package kr.hhplus.be.server.domain.utils

import kr.hhplus.be.server.domain.model.balance.MemberBalance
import kr.hhplus.be.server.domain.model.coupon.CouponIssuance
import kr.hhplus.be.server.domain.model.coupon.CouponOwner
import kr.hhplus.be.server.domain.model.coupon.CouponSummary
import kr.hhplus.be.server.domain.model.coupon.policy.CouponIssuancePolicy
import kr.hhplus.be.server.domain.model.member.Member
import kr.hhplus.be.server.domain.model.order.OrderItem
import kr.hhplus.be.server.domain.model.order.OrderSummary
import kr.hhplus.be.server.domain.model.product.ProductSummary
import java.time.LocalDateTime

object StubFactory {
    // ---------------- Coupon ----------------
    fun couponSummary(
        id: Long? = 1L,
        name: String = "테스트쿠폰",
        discountPercentage: Long = 10L,
        expiredAt: LocalDateTime = LocalDateTime.of(2025, 1, 31, 0, 0, 0),
    ): CouponSummary =
        CouponSummary(
            id = id,
            name = name,
            discountPercentage = discountPercentage,
            expiredAt = expiredAt,
        )

    fun couponOwner(
        id: Long? = 1L,
        couponSummary: CouponSummary = couponSummary(),
        memberId: Long = 1L,
        usingAt: LocalDateTime? = null,
    ): CouponOwner =
        CouponOwner(
            id = id,
            couponSummary = couponSummary,
            memberId = memberId,
            usingAt = usingAt,
        )

    fun couponIssuance(
        id: Long? = 100L,
        couponSummary: CouponSummary,
        policy: CouponIssuancePolicy,
        issuedCount: Int = 0,
        maxCount: Int? = 10,
        startAt: LocalDateTime = LocalDateTime.of(2025, 1, 10, 11, 0, 0),
        endAt: LocalDateTime = LocalDateTime.of(2025, 1, 10, 13, 0, 0),
    ): CouponIssuance =
        CouponIssuance(
            id = id,
            couponSummary = couponSummary,
            policy = policy,
            issuedCount = issuedCount,
            maxCount = maxCount,
            startAt = startAt,
            endAt = endAt,
        )

    // ---------------- Member ----------------
    fun member(
        id: Long? = 1L,
        email: String = "user@test.com",
        pwd: String = "password",
    ): Member = Member(id = id, email = email, pwd = pwd)

    // ---------------- MemberBalance ----------------
    fun memberBalance(
        id: Long? = 1L,
        balance: Long = 10_000L,
        member: Member = member(),
    ): MemberBalance = MemberBalance(id = id, balance = balance, member = member)

    // ---------------- Product ----------------
    fun productSummary(
        id: Long? = 1L,
        name: String = "상품명",
        price: Int = 1000,
        stockQuantity: Int = 100,
    ): ProductSummary = ProductSummary(id = id, name = name, price = price, stockQuantity = stockQuantity)

    // ---------------- Order ----------------
    fun orderSummary(
        id: Long? = 1L,
        memberId: Long,
        orderItemList: List<OrderItem> = emptyList(),
    ): OrderSummary = OrderSummary(id = id, memberId = memberId, orderItems = orderItemList)

    fun orderItem(
        id: Long? = 1L,
        orderSummaryId: Long,
        productId: Long = 1L,
        quantity: Int = 1,
        price: Int = 1000,
    ): OrderItem =
        OrderItem(
            id = id,
            orderSummaryId = orderSummaryId,
            productSummaryId = productId,
            quantity = quantity,
            price = price,
        )

    /**
     * 편의: 주문 + 아이템들을 한 번에 구성 (아이템이 주문 참조 필요해서 2단계 생성)
     */
    fun orderWithItems(
        id: Long = 1L,
        memberId: Long,
        itemsSpec: List<Triple<Long, Int, Int>> = listOf(Triple(1L, 1, 1000)), // (productId, quantity, price)
    ): OrderSummary {
        val items =
            itemsSpec.mapIndexed { idx, (productId, quantity, price) ->
                orderItem(
                    id = idx + 1L,
                    orderSummaryId = id,
                    productId = productId,
                    quantity = quantity,
                    price = price,
                )
            }
        return orderSummary(id = id, memberId = memberId, orderItemList = items)
    }
}
