package kr.hhplus.be.server.common.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("test")
@Configuration
class TestRedissonConfig {
    @Bean
    @Primary
    @DependsOn("TestDockerComposeContainer")
    fun redissonClient(): RedissonClient {
        val config = Config()
        val host = TestDockerComposeContainer.getRedisHost()
        val port = TestDockerComposeContainer.getRedisMappedPort()
        config.useSingleServer().address = "redis://$host:$port"
        return Redisson.create(config)
    }
}
