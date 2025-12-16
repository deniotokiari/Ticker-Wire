package pl.deniotokiari.tickerwire.common.data.cache

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersistentCacheTest {

    @Serializable
    data class TestData(val id: String, val value: Int)

    private class FakeKeyValueLocalDataSource {
        private val storage = mutableMapOf<String, Any>()

        fun <T> getValue(key: String, kSerializer: KSerializer<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return storage[key] as? T
        }

        fun <T> setValue(key: String, value: T, kSerializer: KSerializer<T>) {
            storage[key] = value as Any
        }

        fun <T> removeValue(key: String, kSerializer: KSerializer<T>) {
            storage.remove(key)
        }

        fun remove(key: String) {
            storage.remove(key)
        }

        fun clear() {
            storage.clear()
        }

        fun keys(): Set<String> = storage.keys.toSet()
    }

    private class TestablePersistentCache<T>(
        private val ttl: Long,
        private val keyValueLocalDataSource: FakeKeyValueLocalDataSource,
        private val kSerializer: KSerializer<T>,
    ) : CacheManager<T> {
        override fun get(key: String, ttlSkip: Boolean): T? {
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

    private lateinit var keyValueLocalDataSource: FakeKeyValueLocalDataSource
    private lateinit var cache: TestablePersistentCache<TestData>

    @BeforeTest
    fun setup() {
        keyValueLocalDataSource = FakeKeyValueLocalDataSource()
        cache = TestablePersistentCache(
            ttl = 60_000L, // 1 minute
            keyValueLocalDataSource = keyValueLocalDataSource,
            kSerializer = serializer()
        )
    }

    @Test
    fun getReturnsNullWhenKeyDoesNotExist() {
        val result = cache.get("non_existent", ttlSkip = false)

        assertNull(result)
    }

    @Test
    fun putAndGetStoresAndRetrievesData() {
        val testData = TestData("test", 42)

        cache.put("key1", testData, ttlSkip = false)
        val result = cache.get("key1", ttlSkip = false)

        assertEquals(testData, result)
    }

    @Test
    fun getWithTtlSkipReturnsExpiredEntries() {
        val testData = TestData("test", 42)

        cache.put("key1", testData, ttlSkip = true)
        val result = cache.get("key1", ttlSkip = true)

        assertEquals(testData, result)
    }

    @Test
    fun putOverwritesExistingValue() {
        val testData1 = TestData("test1", 42)
        val testData2 = TestData("test2", 100)

        cache.put("key1", testData1, ttlSkip = false)
        cache.put("key1", testData2, ttlSkip = false)

        val result = cache.get("key1", ttlSkip = false)
        assertEquals(testData2, result)
    }

    @Test
    fun clearRemovesAllEntries() {
        val testData1 = TestData("test1", 42)
        val testData2 = TestData("test2", 100)

        cache.put("key1", testData1, ttlSkip = false)
        cache.put("key2", testData2, ttlSkip = false)

        cache.clear()

        assertNull(cache.get("key1", ttlSkip = false))
        assertNull(cache.get("key2", ttlSkip = false))
    }

    @Test
    fun getWithMultipleKeys() {
        val testData1 = TestData("test1", 42)
        val testData2 = TestData("test2", 100)
        val testData3 = TestData("test3", 200)

        cache.put("key1", testData1, ttlSkip = false)
        cache.put("key2", testData2, ttlSkip = false)
        cache.put("key3", testData3, ttlSkip = false)

        assertEquals(testData1, cache.get("key1", ttlSkip = false))
        assertEquals(testData2, cache.get("key2", ttlSkip = false))
        assertEquals(testData3, cache.get("key3", ttlSkip = false))
    }

    @Test
    fun putWithTtlSkipDoesNotEvictExpired() {
        val testData1 = TestData("test1", 42)
        val testData2 = TestData("test2", 100)

        cache.put("key1", testData1, ttlSkip = true)
        cache.put("key2", testData2, ttlSkip = true)

        assertEquals(testData1, cache.get("key1", ttlSkip = true))
        assertEquals(testData2, cache.get("key2", ttlSkip = true))
    }
}

