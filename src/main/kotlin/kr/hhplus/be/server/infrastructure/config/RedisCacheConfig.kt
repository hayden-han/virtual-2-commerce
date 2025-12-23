package kr.hhplus.be.server.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@EnableCaching
@Configuration
class RedisCacheConfig {
    companion object {
        const val CACHE_PRODUCTS = "products"
        const val CACHE_TOP_SELLING_PRODUCTS = "topSellingProducts"
    }

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val objectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                activateDefaultTyping(
                    BasicPolymorphicTypeValidator
                        .builder()
                        .allowIfBaseType(Any::class.java)
                        .build(),
                    ObjectMapper.DefaultTyping.EVERYTHING,
                    com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY,
                )
            }

        val defaultCacheConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
                ).serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJackson2JsonRedisSerializer(objectMapper),
                    ),
                ).entryTtl(Duration.ofMinutes(10)) // 기본 TTL: 10분

        val cacheConfigMap =
            mapOf(
                CACHE_PRODUCTS to defaultCacheConfig.entryTtl(Duration.ofMinutes(30)), // 상품 목록: 30분
                CACHE_TOP_SELLING_PRODUCTS to defaultCacheConfig.entryTtl(Duration.ofMinutes(15)), // 인기 상품: 15분
            )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigMap)
            .build()
    }
}
