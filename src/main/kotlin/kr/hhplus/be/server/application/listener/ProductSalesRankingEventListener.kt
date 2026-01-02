package kr.hhplus.be.server.application.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.hhplus.be.server.application.usecase.product.TopSellingProductUseCase
import kr.hhplus.be.server.application.vo.PlaceOrderResultVO
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductSalesRankingEventListener(
    private val topSellingProductUseCase: TopSellingProductUseCase,
) {
    private val logger = KotlinLogging.logger {}

    // TODO: 주문과 결합이 되어있다. 향후 메시징 인프라가 도입이 되면 비동기로 분리한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePlaceOrderEvent(placeOrderResult: PlaceOrderResultVO) {
        logger.debug { "상품 판매 랭킹 업데이트: orderId=${placeOrderResult.orderId}, orderDate=${placeOrderResult.orderDate}" }

        placeOrderResult.orderItems.forEach { orderItem ->
            topSellingProductUseCase.recordSales(
                productId = orderItem.productSummaryId,
                quantity = orderItem.quantity,
                salesDate = placeOrderResult.orderDate,
            )
        }
    }
}
