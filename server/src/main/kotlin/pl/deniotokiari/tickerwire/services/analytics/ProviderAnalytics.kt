package pl.deniotokiari.tickerwire.services.analytics

import kotlinx.serialization.Serializable
import pl.deniotokiari.tickerwire.models.Provider
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Analytics data for stock provider usage
 */
@Serializable
data class ProviderAnalyticsSnapshot(
    val timestamp: Long = Instant.now().toEpochMilli(),
    val uptime: Long = 0,
    val providerStats: Map<String, ProviderStats> = emptyMap(),
    val operationStats: OperationStats = OperationStats(),
    val cacheStats: CacheStats = CacheStats(),
)

@Serializable
data class ProviderStats(
    val provider: String,
    val totalSelections: Long = 0,
    val totalFailures: Long = 0,
    val searchSelections: Long = 0,
    val newsSelections: Long = 0,
    val infoSelections: Long = 0,
    val searchFailures: Long = 0,
    val newsFailures: Long = 0,
    val infoFailures: Long = 0,
    val successRate: Double = 0.0,
    val lastSelectedAt: Long? = null,
    val lastFailedAt: Long? = null,
)

@Serializable
data class OperationStats(
    val totalSearchRequests: Long = 0,
    val totalNewsRequests: Long = 0,
    val totalInfoRequests: Long = 0,
    val totalRequests: Long = 0,
    val averageSearchLatencyMs: Double = 0.0,
    val averageNewsLatencyMs: Double = 0.0,
    val averageInfoLatencyMs: Double = 0.0,
)

@Serializable
data class CacheStats(
    val searchCacheHits: Long = 0,
    val searchCacheMisses: Long = 0,
    val searchCacheHitRate: Double = 0.0,
    val newsCacheHits: Long = 0,
    val newsCacheMisses: Long = 0,
    val newsCacheHitRate: Double = 0.0,
    val infoCacheHits: Long = 0,
    val infoCacheMisses: Long = 0,
    val infoCacheHitRate: Double = 0.0,
    val totalCacheHits: Long = 0,
    val totalCacheMisses: Long = 0,
    val overallCacheHitRate: Double = 0.0,
)

/**
 * Operation type for analytics tracking
 */
enum class OperationType {
    SEARCH,
    NEWS,
    INFO
}

/**
 * Service for tracking and reporting provider analytics
 */
class ProviderAnalyticsService {
    private val startTime = Instant.now()

    // Provider selection counters
    private val providerSelections = ConcurrentHashMap<Provider, ConcurrentHashMap<OperationType, AtomicLong>>()
    private val providerFailures = ConcurrentHashMap<Provider, ConcurrentHashMap<OperationType, AtomicLong>>()
    private val providerLastSelected = ConcurrentHashMap<Provider, AtomicLong>()
    private val providerLastFailed = ConcurrentHashMap<Provider, AtomicLong>()

    // Operation counters
    private val searchRequests = AtomicLong(0)
    private val newsRequests = AtomicLong(0)
    private val infoRequests = AtomicLong(0)

    // Latency tracking (using exponential moving average)
    private val searchLatencySum = AtomicLong(0)
    private val searchLatencyCount = AtomicLong(0)
    private val newsLatencySum = AtomicLong(0)
    private val newsLatencyCount = AtomicLong(0)
    private val infoLatencySum = AtomicLong(0)
    private val infoLatencyCount = AtomicLong(0)

    // Cache counters
    private val searchCacheHits = AtomicLong(0)
    private val searchCacheMisses = AtomicLong(0)
    private val newsCacheHits = AtomicLong(0)
    private val newsCacheMisses = AtomicLong(0)
    private val infoCacheHits = AtomicLong(0)
    private val infoCacheMisses = AtomicLong(0)

    /**
     * Record that a provider was selected for an operation
     */
    fun recordProviderSelection(provider: Provider, operation: OperationType) {
        getOrCreateProviderMap(providerSelections, provider)[operation]!!.incrementAndGet()
        providerLastSelected[provider] = AtomicLong(Instant.now().toEpochMilli())

        when (operation) {
            OperationType.SEARCH -> searchRequests.incrementAndGet()
            OperationType.NEWS -> newsRequests.incrementAndGet()
            OperationType.INFO -> infoRequests.incrementAndGet()
        }
    }

    /**
     * Record that a provider failed for an operation
     */
    fun recordProviderFailure(provider: Provider, operation: OperationType) {
        getOrCreateProviderMap(providerFailures, provider)[operation]!!.incrementAndGet()
        providerLastFailed[provider] = AtomicLong(Instant.now().toEpochMilli())
    }

    /**
     * Record operation latency
     */
    fun recordLatency(operation: OperationType, latencyMs: Long) {
        when (operation) {
            OperationType.SEARCH -> {
                searchLatencySum.addAndGet(latencyMs)
                searchLatencyCount.incrementAndGet()
            }
            OperationType.NEWS -> {
                newsLatencySum.addAndGet(latencyMs)
                newsLatencyCount.incrementAndGet()
            }
            OperationType.INFO -> {
                infoLatencySum.addAndGet(latencyMs)
                infoLatencyCount.incrementAndGet()
            }
        }
    }

    /**
     * Record cache hit
     */
    fun recordCacheHit(operation: OperationType) {
        when (operation) {
            OperationType.SEARCH -> searchCacheHits.incrementAndGet()
            OperationType.NEWS -> newsCacheHits.incrementAndGet()
            OperationType.INFO -> infoCacheHits.incrementAndGet()
        }
    }

    /**
     * Record cache miss
     */
    fun recordCacheMiss(operation: OperationType) {
        when (operation) {
            OperationType.SEARCH -> searchCacheMisses.incrementAndGet()
            OperationType.NEWS -> newsCacheMisses.incrementAndGet()
            OperationType.INFO -> infoCacheMisses.incrementAndGet()
        }
    }

    /**
     * Get current analytics snapshot
     */
    fun getSnapshot(): ProviderAnalyticsSnapshot {
        val uptimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli()

        // Build provider stats
        val allProviders = (providerSelections.keys + providerFailures.keys).toSet()
        val providerStats = allProviders.associate { provider ->
            val selections = providerSelections[provider] ?: emptyMap<OperationType, AtomicLong>()
            val failures = providerFailures[provider] ?: emptyMap<OperationType, AtomicLong>()

            val searchSel = selections[OperationType.SEARCH]?.get() ?: 0
            val newsSel = selections[OperationType.NEWS]?.get() ?: 0
            val infoSel = selections[OperationType.INFO]?.get() ?: 0
            val totalSel = searchSel + newsSel + infoSel

            val searchFail = failures[OperationType.SEARCH]?.get() ?: 0
            val newsFail = failures[OperationType.NEWS]?.get() ?: 0
            val infoFail = failures[OperationType.INFO]?.get() ?: 0
            val totalFail = searchFail + newsFail + infoFail

            val successRate = if (totalSel > 0) {
                ((totalSel - totalFail).toDouble() / totalSel) * 100
            } else {
                100.0
            }

            provider.name to ProviderStats(
                provider = provider.name,
                totalSelections = totalSel,
                totalFailures = totalFail,
                searchSelections = searchSel,
                newsSelections = newsSel,
                infoSelections = infoSel,
                searchFailures = searchFail,
                newsFailures = newsFail,
                infoFailures = infoFail,
                successRate = successRate,
                lastSelectedAt = providerLastSelected[provider]?.get(),
                lastFailedAt = providerLastFailed[provider]?.get(),
            )
        }

        // Build operation stats
        val totalSearchLatency = searchLatencySum.get()
        val totalSearchCount = searchLatencyCount.get()
        val totalNewsLatency = newsLatencySum.get()
        val totalNewsCount = newsLatencyCount.get()
        val totalInfoLatency = infoLatencySum.get()
        val totalInfoCount = infoLatencyCount.get()

        val operationStats = OperationStats(
            totalSearchRequests = searchRequests.get(),
            totalNewsRequests = newsRequests.get(),
            totalInfoRequests = infoRequests.get(),
            totalRequests = searchRequests.get() + newsRequests.get() + infoRequests.get(),
            averageSearchLatencyMs = if (totalSearchCount > 0) totalSearchLatency.toDouble() / totalSearchCount else 0.0,
            averageNewsLatencyMs = if (totalNewsCount > 0) totalNewsLatency.toDouble() / totalNewsCount else 0.0,
            averageInfoLatencyMs = if (totalInfoCount > 0) totalInfoLatency.toDouble() / totalInfoCount else 0.0,
        )

        // Build cache stats
        val sHits = searchCacheHits.get()
        val sMisses = searchCacheMisses.get()
        val nHits = newsCacheHits.get()
        val nMisses = newsCacheMisses.get()
        val iHits = infoCacheHits.get()
        val iMisses = infoCacheMisses.get()

        val totalHits = sHits + nHits + iHits
        val totalMisses = sMisses + nMisses + iMisses

        val cacheStats = CacheStats(
            searchCacheHits = sHits,
            searchCacheMisses = sMisses,
            searchCacheHitRate = calculateHitRate(sHits, sMisses),
            newsCacheHits = nHits,
            newsCacheMisses = nMisses,
            newsCacheHitRate = calculateHitRate(nHits, nMisses),
            infoCacheHits = iHits,
            infoCacheMisses = iMisses,
            infoCacheHitRate = calculateHitRate(iHits, iMisses),
            totalCacheHits = totalHits,
            totalCacheMisses = totalMisses,
            overallCacheHitRate = calculateHitRate(totalHits, totalMisses),
        )

        return ProviderAnalyticsSnapshot(
            uptime = uptimeMs,
            providerStats = providerStats,
            operationStats = operationStats,
            cacheStats = cacheStats,
        )
    }

    /**
     * Reset all analytics counters
     */
    fun reset() {
        providerSelections.clear()
        providerFailures.clear()
        providerLastSelected.clear()
        providerLastFailed.clear()

        searchRequests.set(0)
        newsRequests.set(0)
        infoRequests.set(0)

        searchLatencySum.set(0)
        searchLatencyCount.set(0)
        newsLatencySum.set(0)
        newsLatencyCount.set(0)
        infoLatencySum.set(0)
        infoLatencyCount.set(0)

        searchCacheHits.set(0)
        searchCacheMisses.set(0)
        newsCacheHits.set(0)
        newsCacheMisses.set(0)
        infoCacheHits.set(0)
        infoCacheMisses.set(0)
    }

    private fun getOrCreateProviderMap(
        map: ConcurrentHashMap<Provider, ConcurrentHashMap<OperationType, AtomicLong>>,
        provider: Provider
    ): ConcurrentHashMap<OperationType, AtomicLong> {
        return map.computeIfAbsent(provider) {
            ConcurrentHashMap<OperationType, AtomicLong>().apply {
                OperationType.entries.forEach { op ->
                    put(op, AtomicLong(0))
                }
            }
        }
    }

    private fun calculateHitRate(hits: Long, misses: Long): Double {
        val total = hits + misses
        return if (total > 0) {
            (hits.toDouble() / total) * 100
        } else {
            0.0
        }
    }
}

