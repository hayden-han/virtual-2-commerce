package kr.hhplus.be.server.infrastructure.persistence.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kr.hhplus.be.server.domain.model.product.ProductSummary
import kr.hhplus.be.server.infrastructure.persistence.config.CreatedAndUpdatedAtAuditEntity

@Entity
@Table(name = "product_summary")
class ProductSummaryJpaEntity(
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val price: Int,
    @Column(nullable = false) var stockQuantity: Int,
) : CreatedAndUpdatedAtAuditEntity() {
    fun toDomain(): ProductSummary =
        ProductSummary(
            id = id,
            name = name,
            price = price,
            stockQuantity = stockQuantity,
        )

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    companion object {
        fun from(domain: ProductSummary): ProductSummaryJpaEntity =
            ProductSummaryJpaEntity(
                name = domain.name,
                price = domain.price,
                stockQuantity = domain.stockQuantity,
            ).apply {
                id = domain.id
            }
    }
}
