package pl.deniotokiari.tickerwire.common.data.cache

import kotlinx.serialization.KSerializer
import pl.deniotokiari.tickerwire.common.data.store.KeyValueLocalDataSource

class PersistentCache<T>(
    private val ttl: Long,
    private val keyValueLocalDataSource: KeyValueLocalDataSource,
    private val kSerializer: KSerializer<T>,
) : CacheManager<T> {
    @Suppress("USELESS_CAST")
    override fun get(key: String, ttlSkip: Boolean): T? {
        val key = getKey(key)
        val entry = keyValueLocalDataSource.getValue(
            key = key,
            kSerializer = CacheEntry.serializer(kSerializer),
        ) ?: return null

        return if (ttlSkip || entry.isValid()) {
            entry.data
        } else {
            keyValueLocalDataSource.removeValue(key, kSerializer)

            null
        }
    }

    override fun put(key: String, data: T, ttlSkip: Boolean) {
        val key = getKey(key)

        if (!ttlSkip) {
            evictExpired()
        }

        keyValueLocalDataSource.setValue(
            key = key,
            value = CacheEntry(data = data, ttl = ttl),
            kSerializer = CacheEntry.serializer(kSerializer),
        )
    }

    override fun clear() {
        keyValueLocalDataSource.clear()
    }

    private fun getKey(key: String): String = key.hashCode().toString()

    private fun evictExpired() {
        for (key in keyValueLocalDataSource.keys()) {
            val entry = keyValueLocalDataSource.getValue(key, CacheEntry.serializer(kSerializer))

            if (entry != null && entry.isExpired()) {
                keyValueLocalDataSource.removeValue(key, CacheEntry.serializer(kSerializer))
            } else if (entry == null) {
                keyValueLocalDataSource.remove(key)
            }
        }
    }
}
