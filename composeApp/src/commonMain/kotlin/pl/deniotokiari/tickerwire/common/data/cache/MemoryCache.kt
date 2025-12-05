package pl.deniotokiari.tickerwire.common.data.cache

class MemoryCache<T>(
    private val limit: Int,
    private val ttl: Long,
) : CacheManager<T> {
    private val cache = mutableMapOf<String, CacheEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun get(key: String, ttlSkip: Boolean): T? {
        val key = getKey(key)
        val entry = cache[key] ?: return null

        return if (ttlSkip || entry.isValid()) {
            cache.remove(key)
            cache[key] = entry

            entry.data as T
        } else {
            cache.remove(key)

            null
        }
    }

    override fun put(key: String, data: T, ttlSkip: Boolean) {
        val key = getKey(key)

        if (!ttlSkip) {
            evictExpired()

            if (cache.size >= limit && !cache.contains(key)) {
                val firstKey = cache.keys.firstOrNull()

                firstKey?.let(cache::remove)
            }
        }

        cache[key] = CacheEntry(
            data = data,
            ttl = ttl,
        )
    }

    override fun clear() {
        cache.clear()
    }

    private fun getKey(key: String): String = key.hashCode().toString()

    fun evictExpired() {
        cache.entries.removeAll { it.value.isExpired() }
    }
}
