package kr.hhplus.be.server.infrastructure.persistence.config

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class RoutingDataSource : AbstractRoutingDataSource() {
    override fun determineCurrentLookupKey(): Any? = DataSourceContextHolder.get()
}
