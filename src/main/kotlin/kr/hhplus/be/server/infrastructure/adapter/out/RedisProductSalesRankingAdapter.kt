package kr.hhplus.be.server.infrastructure.adapter.out

import kr.hhplus.be.server.application.port.out.ProductSalesRankingOutput
import kr.hhplus.be.server.domain.model.product.TopSellingProductRanking
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class RedisProductSalesRankingAdapter(
    private val redisTemplate: StringRedisTemplate,
) : ProductSalesRankingOutput {
    companion object {
        private const val RANKING_KEY_PREFIX = "product:sales:ranking"
        private const val TEMP_KEY_PREFIX = "product:sales:ranking:temp"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    override fun incrementSalesCount(
        productId: Long,
        quantity: Int,
        salesDate: LocalDate,
    ) {
        val key = buildDailyKey(salesDate)
        redisTemplate.opsForZSet().incrementScore(
            key,
            productId.toString(),
            quantity.toDouble(),
        )
    }

    override fun getTopRanking(
        startDate: LocalDate,
        endDate: LocalDate,
        limit: Int,
    ): TopSellingProductRanking {
        val dailyKeys = generateDailyKeys(startDate, endDate)

        if (dailyKeys.isEmpty()) {
            return TopSellingProductRanking.empty()
        }

        // 단일 날짜인 경우 ZUNIONSTORE 없이 바로 조회
        if (dailyKeys.size == 1) {
            return getTopRankingFromKey(dailyKeys.first(), limit)
        }

        // 여러 날짜인 경우 ZUNIONSTORE로 합산 후 조회
        val tempKey = "$TEMP_KEY_PREFIX:${System.currentTimeMillis()}"
        try {
            redisTemplate.opsForZSet().unionAndStore(
                dailyKeys.first(),
                dailyKeys.drop(1),
                tempKey,
            )
            return getTopRankingFromKey(tempKey, limit)
        } finally {
            redisTemplate.delete(tempKey)
        }
    }

    private fun buildDailyKey(date: LocalDate): String {
        return "$RANKING_KEY_PREFIX:${date.format(DATE_FORMATTER)}"
    }

    private fun generateDailyKeys(startDate: LocalDate, endDate: LocalDate): List<String> {
        val keys = mutableListOf<String>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            keys.add(buildDailyKey(current))
            current = current.plusDays(1)
        }
        return keys
    }

    private fun getTopRankingFromKey(key: String, limit: Int): TopSellingProductRanking {
        val results = redisTemplate
            .opsForZSet()
            .reverseRangeWithScores(key, 0, (limit - 1).toLong())
            ?: return TopSellingProductRanking.empty()

        val rawData = results.mapNotNull { typedTuple ->
            val productId = typedTuple.value?.toLongOrNull()
            val salesCount = typedTuple.score?.toInt()
            if (productId != null && salesCount != null) {
                productId to salesCount
            } else {
                null
            }
        }

        return TopSellingProductRanking.from(rawData)
    }
}
