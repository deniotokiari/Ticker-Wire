package pl.deniotokiari.tickerwire.common.data.cache

interface CacheManager<T> {
    fun get(key: String, ttlSkip: Boolean): T?
    fun put(key: String, data: T, ttlSkip: Boolean)
    fun clear()
}
