package kr.hhplus.be.server.application.interactor.product

import kr.hhplus.be.server.application.event.ProductCacheEvictEvent
import kr.hhplus.be.server.application.port.out.ProductSummaryOutput
import kr.hhplus.be.server.application.usecase.product.ProductQuantityUseCase
import kr.hhplus.be.server.application.vo.PlaceOrderItemVO
import kr.hhplus.be.server.domain.exception.ConflictResourceException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class ProductQuantityInteractor(
    private val productSummaryOutput: ProductSummaryOutput,
    private val eventPublisher: ApplicationEventPublisher,
) : ProductQuantityUseCase {
    /**
     * 주문된 상품들의 재고 감소.
     * 각 상품별로 분산 락과 조건부 UPDATE를 통해 동시성을 보장한다.
     * (분산 락은 Infrastructure Adapter에서 처리)
     */
    override fun reduceBy(orderItems: List<PlaceOrderItemVO>) {
        // 상품명 조회 (에러 메시지용)
        val productIds = orderItems.map { it.productSummaryId }
        val products = productSummaryOutput.findAllInIds(productIds)
        val productNameMap = products.associate { it.id to it.name }

        // 상품별 재고 차감 (Adapter에서 분산 락 + 조건부 UPDATE 처리)
        orderItems.forEach { item ->
            val success = productSummaryOutput.reduceStock(item.productSummaryId, item.quantity)
            if (!success) {
                val productName = productNameMap[item.productSummaryId] ?: "알 수 없는 상품"
                throw ConflictResourceException(
                    message = "'$productName'의 재고가 부족합니다.",
                    clue = mapOf(
                        "productSummaryId" to item.productSummaryId,
                        "requestedQuantity" to item.quantity,
                    ),
                )
            }
        }

        // 캐시 무효화 이벤트 발행
        eventPublisher.publishEvent(ProductCacheEvictEvent(reason = "재고 차감"))
    }
}
