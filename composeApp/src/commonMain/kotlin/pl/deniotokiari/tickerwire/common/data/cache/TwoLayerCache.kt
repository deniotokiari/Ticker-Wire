package pl.deniotokiari.tickerwire.common.data.cache

import pl.deniotokiari.tickerwire.common.etc.Logger

private const val LOGGER_TAG = "TwoLayerCache"

class TwoLayerCache<T>(
    private val memoryCache: MemoryCache<T>,
    private val persistentCache: PersistentCache<T>,
    private val logger: Logger,
) {
    suspend fun getOrFetch(key: String, ttlSkip: Boolean, fetch: suspend () -> T): T {
        memoryCache.get(key, ttlSkip)?.let { result ->
            logger.d(LOGGER_TAG, "L1: $key, ttlSkip: $ttlSkip")

            return result
        }

        persistentCache.get(key, ttlSkip)?.let { result ->
            // Only put into memory cache if ttlSkip is false (respecting TTL)
            // If ttlSkip is true, we're returning expired data and shouldn't refresh its timestamp
            // by putting it into memory cache with a fresh timestamp
            if (!ttlSkip) {
                memoryCache.put(key, result, ttlSkip)
            }

            logger.d(LOGGER_TAG, "L2: $key, ttlSkip: $ttlSkip")

            return result
        }

        val result = fetch()

        logger.d(LOGGER_TAG, "L3: $key")

        memoryCache.put(key, result, false)
        persistentCache.put(key, result, false)

        return result
    }

    fun put(key: String, data: T) {
        memoryCache.put(key, data, false)
        persistentCache.put(key, data, false)
    }

    fun get(key: String, ttlSkip: Boolean): T? {
        memoryCache.get(key, ttlSkip)?.let { result ->
            logger.d(LOGGER_TAG, "L1: $key, ttlSkip: $ttlSkip")

            return result
        }

        persistentCache.get(key, ttlSkip)?.let { result ->
            // Only put into memory cache if ttlSkip is false (respecting TTL)
            // If ttlSkip is true, we're returning expired data and shouldn't refresh its timestamp
            // by putting it into memory cache with a fresh timestamp
            if (!ttlSkip) {
                memoryCache.put(key, result, ttlSkip)
            }

            logger.d(LOGGER_TAG, "L2: $key, ttlSkip: $ttlSkip")

            return result
        }

        return null
    }

    fun clear() {
        memoryCache.clear()
        persistentCache.clear()
    }
}
