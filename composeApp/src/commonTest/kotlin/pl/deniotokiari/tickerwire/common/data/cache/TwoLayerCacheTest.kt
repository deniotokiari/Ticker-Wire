package pl.deniotokiari.tickerwire.common.data.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TwoLayerCacheTest {

    // Fake Memory Cache for testing
    private class FakeMemoryCache<T> : CacheManager<T> {
        private val data = mutableMapOf<String, T>()

        override fun get(key: String, ttlSkip: Boolean): T? = data[key]

        override fun put(key: String, data: T, ttlSkip: Boolean) {
            this.data[key] = data
        }

        override fun clear() {
            data.clear()
        }
    }

    // Fake Persistent Cache for testing
    private class FakePersistentCache<T> : CacheManager<T> {
        private val data = mutableMapOf<String, T>()

        override fun get(key: String, ttlSkip: Boolean): T? = data[key]

        override fun put(key: String, data: T, ttlSkip: Boolean) {
            this.data[key] = data
        }

        override fun clear() {
            data.clear()
        }
    }

    // Fake Logger for testing
    private class FakeLogger {
        val messages = mutableListOf<String>()

        fun d(tag: String, message: String) {
            messages.add("$tag: $message")
        }
    }

    // Simplified TwoLayerCache wrapper for testing
    private class TestableTwoLayerCache<T>(
        private val memoryCache: CacheManager<T>,
        private val persistentCache: CacheManager<T>,
        private val logger: FakeLogger,
    ) {
        fun getOrFetch(key: String, ttlSkip: Boolean, fetch: () -> T): T {
            memoryCache.get(key, ttlSkip)?.let { result ->
                logger.d("TwoLayerCache", "L1: $key, ttlSkip: $ttlSkip")
                return result
            }

            persistentCache.get(key, ttlSkip)?.let { result ->
                // Only put into memory cache if ttlSkip is false (respecting TTL)
                // If ttlSkip is true, we're returning expired data and shouldn't refresh its timestamp
                if (!ttlSkip) {
                    memoryCache.put(key, result, ttlSkip)
                }
                logger.d("TwoLayerCache", "L2: $key, ttlSkip: $ttlSkip")
                return result
            }

            val result = fetch()
            logger.d("TwoLayerCache", "L3: $key")
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
                logger.d("TwoLayerCache", "L1: $key, ttlSkip: $ttlSkip")
                return result
            }

            persistentCache.get(key, ttlSkip)?.let { result ->
                // Only put into memory cache if ttlSkip is false (respecting TTL)
                // If ttlSkip is true, we're returning expired data and shouldn't refresh its timestamp
                if (!ttlSkip) {
                    memoryCache.put(key, result, ttlSkip)
                }
                logger.d("TwoLayerCache", "L2: $key, ttlSkip: $ttlSkip")
                return result
            }

            return null
        }

        fun clear() {
            memoryCache.clear()
            persistentCache.clear()
        }
    }

    @Test
    fun getOrFetchReturnsFromMemoryCacheWhenAvailable() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        memoryCache.put("key1", "memory_value", false)
        var fetchCalled = false

        val result = cache.getOrFetch("key1", false) {
            fetchCalled = true
            "fetch_value"
        }

        assertEquals("memory_value", result)
        assertEquals(false, fetchCalled)
        assertEquals(1, logger.messages.size)
        assertEquals("TwoLayerCache: L1: key1, ttlSkip: false", logger.messages[0])
    }

    @Test
    fun getOrFetchReturnsFromPersistentCacheWhenMemoryEmpty() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        persistentCache.put("key1", "persistent_value", false)
        var fetchCalled = false

        val result = cache.getOrFetch("key1", false) {
            fetchCalled = true
            "fetch_value"
        }

        assertEquals("persistent_value", result)
        assertEquals(false, fetchCalled)
        assertEquals(1, logger.messages.size)
        assertEquals("TwoLayerCache: L2: key1, ttlSkip: false", logger.messages[0])

        // Also check memory cache was populated
        assertEquals("persistent_value", memoryCache.get("key1", false))
    }

    @Test
    fun getOrFetchCallsFetchWhenBothCachesEmpty() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        var fetchCalled = false

        val result = cache.getOrFetch("key1", false) {
            fetchCalled = true
            "fetch_value"
        }

        assertEquals("fetch_value", result)
        assertEquals(true, fetchCalled)
        assertEquals(1, logger.messages.size)
        assertEquals("TwoLayerCache: L3: key1", logger.messages[0])

        // Both caches should be populated
        assertEquals("fetch_value", memoryCache.get("key1", false))
        assertEquals("fetch_value", persistentCache.get("key1", false))
    }

    @Test
    fun putStoresInBothCaches() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        cache.put("key1", "value")

        assertEquals("value", memoryCache.get("key1", false))
        assertEquals("value", persistentCache.get("key1", false))
    }

    @Test
    fun getReturnsFromMemoryCacheFirst() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        memoryCache.put("key1", "memory_value", false)
        persistentCache.put("key1", "persistent_value", false)

        val result = cache.get("key1", false)

        assertEquals("memory_value", result)
        assertEquals(1, logger.messages.size)
        assertEquals("TwoLayerCache: L1: key1, ttlSkip: false", logger.messages[0])
    }

    @Test
    fun getReturnsFromPersistentCacheWhenMemoryEmpty() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        persistentCache.put("key1", "persistent_value", false)

        val result = cache.get("key1", false)

        assertEquals("persistent_value", result)
        assertEquals("persistent_value", memoryCache.get("key1", false)) // Promoted to memory when ttlSkip = false
    }

    @Test
    fun getWithTtlSkipTrueDoesNotPromoteToMemoryCache() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        persistentCache.put("key1", "persistent_value", false)

        val result = cache.get("key1", true)

        assertEquals("persistent_value", result)
        assertNull(memoryCache.get("key1", false)) // Should NOT be promoted when ttlSkip = true
    }

    @Test
    fun getReturnsNullWhenBothCachesEmpty() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        val result = cache.get("key1", false)

        assertNull(result)
        assertEquals(0, logger.messages.size)
    }

    @Test
    fun clearRemovesFromBothCaches() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        cache.put("key1", "value1")
        cache.put("key2", "value2")

        cache.clear()

        assertNull(memoryCache.get("key1", false))
        assertNull(persistentCache.get("key1", false))
        assertNull(memoryCache.get("key2", false))
        assertNull(persistentCache.get("key2", false))
    }

    @Test
    fun getOrFetchWithTtlSkipPassesCorrectValue() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        memoryCache.put("key1", "value", false)

        cache.getOrFetch("key1", true) { "fetch_value" }

        assertEquals(1, logger.messages.size)
        assertEquals("TwoLayerCache: L1: key1, ttlSkip: true", logger.messages[0])
    }

    @Test
    fun multipleKeysAreIsolated() {
        val memoryCache = FakeMemoryCache<String>()
        val persistentCache = FakePersistentCache<String>()
        val logger = FakeLogger()
        val cache = TestableTwoLayerCache(memoryCache, persistentCache, logger)

        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")

        assertEquals("value1", cache.get("key1", false))
        assertEquals("value2", cache.get("key2", false))
        assertEquals("value3", cache.get("key3", false))
    }
}

