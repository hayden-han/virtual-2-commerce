package kr.hhplus.be.server.infrastructure.persistence.config

object DataSourceContextHolder {
    private val contextHolder = ThreadLocal<DataSourceType>()

    fun set(type: DataSourceType) {
        contextHolder.set(type)
    }

    fun get(): DataSourceType = contextHolder.get() ?: DataSourceType.MASTER

    fun clear() {
        contextHolder.remove()
    }
}
