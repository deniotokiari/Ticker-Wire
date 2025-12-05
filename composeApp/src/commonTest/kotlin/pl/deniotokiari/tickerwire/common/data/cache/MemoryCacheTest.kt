package pl.deniotokiari.tickerwire.common.data.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemoryCacheTest {

    @Test
    fun getReturnsNullForMissingKey() {
        val cache = MemoryCache<String>(limit = 10, ttl = 60_000L)

        val result = cache.get("non_existent_key", ttlSkip = false)

        assertNull(result)
    }

    @Test
    fun putAndGetRetrievesStoredValue() {
        val cache = MemoryCache<String>(limit = 10, ttl = 60_000L)

        cache.put("key1", "value1", ttlSkip = false)
        val result = cache.get("key1", ttlSkip = false)

        assertEquals("value1", result)
    }

    @Test
    fun putOverwritesExistingValue() {
        val cache = MemoryCache<String>(limit = 10, ttl = 60_000L)

        cache.put("key1", "value1", ttlSkip = false)
        cache.put("key1", "value2", ttlSkip = false)
        val result = cache.get("key1", ttlSkip = false)

        assertEquals("value2", result)
    }

    @Test
    fun clearRemovesAllEntries() {
        val cache = MemoryCache<String>(limit = 10, ttl = 60_000L)

        cache.put("key1", "value1", ttlSkip = false)
        cache.put("key2", "value2", ttlSkip = false)
        cache.clear()

        assertNull(cache.get("key1", ttlSkip = false))
        assertNull(cache.get("key2", ttlSkip = false))
    }

    @Test
    fun cacheRespectsLimitByEvictingOldestEntry() {
        val cache = MemoryCache<String>(limit = 2, ttl = 60_000L)

        cache.put("key1", "value1", ttlSkip = false)
        cache.put("key2", "value2", ttlSkip = false)
        cache.put("key3", "value3", ttlSkip = false) // Should evict key1

        assertNull(cache.get("key1", ttlSkip = false))
        assertEquals("value2", cache.get("key2", ttlSkip = false))
        assertEquals("value3", cache.get("key3", ttlSkip = false))
    }

    @Test
    fun ttlSkipTrueOnGetReturnsExpiredEntries() {
        val cache = MemoryCache<String>(limit = 10, ttl = 1L) // Very short TTL

        cache.put("key1", "value1", ttlSkip = true)

        // Even with expired TTL, ttlSkip=true should return the value
        val result = cache.get("key1", ttlSkip = true)

        assertEquals("value1", result)
    }

    @Test
    fun cacheStoresDifferentTypesCorrectly() {
        val stringCache = MemoryCache<String>(limit = 10, ttl = 60_000L)
        val intCache = MemoryCache<Int>(limit = 10, ttl = 60_000L)
        val listCache = MemoryCache<List<String>>(limit = 10, ttl = 60_000L)

        stringCache.put("key", "string value", ttlSkip = false)
        intCache.put("key", 42, ttlSkip = false)
        listCache.put("key", listOf("a", "b", "c"), ttlSkip = false)

        assertEquals("string value", stringCache.get("key", ttlSkip = false))
        assertEquals(42, intCache.get("key", ttlSkip = false))
        assertEquals(listOf("a", "b", "c"), listCache.get("key", ttlSkip = false))
    }

    @Test
    fun getWithTtlSkipFalseReturnsNullForExpiredEntry() {
        val cache = MemoryCache<String>(limit = 10, ttl = 0L) // Zero TTL = immediately expired

        cache.put("key1", "value1", ttlSkip = true) // Put with ttlSkip to avoid immediate eviction

        // Get without ttlSkip should return null for expired entry
        val result = cache.get("key1", ttlSkip = false)

        assertNull(result)
    }

    @Test
    fun multipleGetsReturnSameValue() {
        val cache = MemoryCache<String>(limit = 10, ttl = 60_000L)

        cache.put("key1", "value1", ttlSkip = false)

        val result1 = cache.get("key1", ttlSkip = false)
        val result2 = cache.get("key1", ttlSkip = false)
        val result3 = cache.get("key1", ttlSkip = false)

        assertEquals("value1", result1)
        assertEquals("value1", result2)
        assertEquals("value1", result3)
    }
}

