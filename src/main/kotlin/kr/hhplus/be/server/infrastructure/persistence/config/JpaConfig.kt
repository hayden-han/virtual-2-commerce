package kr.hhplus.be.server.infrastructure.persistence.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories("kr.hhplus.be.server.infrastructure.persistence")
class JpaConfig {
    @Bean
    fun transactionManager(): PlatformTransactionManager = JpaTransactionManager()
}
