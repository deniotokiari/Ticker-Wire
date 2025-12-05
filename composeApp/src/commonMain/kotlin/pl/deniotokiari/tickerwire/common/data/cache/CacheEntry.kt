package pl.deniotokiari.tickerwire.common.data.cache

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val ttl: Long,
) {
    fun isExpired(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - timestamp

        return elapsed >= ttl
    }

    fun isValid(): Boolean = !isExpired()
}
