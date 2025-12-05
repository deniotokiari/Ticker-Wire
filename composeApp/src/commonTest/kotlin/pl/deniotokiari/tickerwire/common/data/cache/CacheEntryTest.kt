package pl.deniotokiari.tickerwire.common.data.cache

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CacheEntryTest {

    @Test
    fun `fresh cache entry should be valid`() {
        val entry = CacheEntry(
            data = "test data",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            ttl = 60_000L // 1 minute
        )

        assertTrue(entry.isValid())
        assertFalse(entry.isExpired())
    }

    @Test
    fun `expired cache entry should not be valid`() {
        val entry = CacheEntry(
            data = "test data",
            timestamp = Clock.System.now().toEpochMilliseconds() - 120_000L, // 2 minutes ago
            ttl = 60_000L // 1 minute TTL
        )

        assertFalse(entry.isValid())
        assertTrue(entry.isExpired())
    }

    @Test
    fun `cache entry at exactly TTL boundary should be expired`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val entry = CacheEntry(
            data = "test data",
            timestamp = now - 60_000L, // exactly 1 minute ago
            ttl = 60_000L // 1 minute TTL
        )

        assertTrue(entry.isExpired())
        assertFalse(entry.isValid())
    }

    @Test
    fun `cache entry well before TTL should be valid`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val entry = CacheEntry(
            data = "test data",
            timestamp = now - 30_000L, // 30 seconds ago (half of TTL)
            ttl = 60_000L // 1 minute TTL
        )

        assertTrue(entry.isValid())
        assertFalse(entry.isExpired())
    }

    @Test
    fun `cache entry with zero TTL should be immediately expired`() {
        val entry = CacheEntry(
            data = "test data",
            timestamp = Clock.System.now().toEpochMilliseconds(),
            ttl = 0L
        )

        assertTrue(entry.isExpired())
        assertFalse(entry.isValid())
    }

    @Test
    fun `cache entry preserves data correctly`() {
        val testData = listOf("item1", "item2", "item3")
        val entry = CacheEntry(
            data = testData,
            ttl = 60_000L
        )

        kotlin.test.assertEquals(testData, entry.data)
    }
}

