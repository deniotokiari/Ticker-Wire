package pl.deniotokiari.tickerwire.services.analytics

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.deniotokiari.tickerwire.models.Provider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Firebase Analytics Service for tracking provider usage
 *
 * Stores analytics events in Firestore collections:
 * - provider_analytics/{date}/events/{eventId} - Individual events
 * - provider_analytics/{date}/aggregates/summary - Daily aggregated stats
 *
 * This allows for:
 * - Real-time event tracking
 * - Daily aggregated statistics
 * - Easy querying and reporting in Firebase Console
 * - Export to BigQuery for advanced analytics
 */
class FirebaseAnalyticsService(
    private val firestore: Firestore,
) {
    private val logger = LoggerFactory.getLogger(FirebaseAnalyticsService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val COLLECTION_ANALYTICS = "provider_analytics"
        private const val SUBCOLLECTION_EVENTS = "events"
        private const val SUBCOLLECTION_AGGREGATES = "aggregates"
        private const val DOC_SUMMARY = "summary"

        // Event types
        const val EVENT_PROVIDER_SELECTED = "provider_selected"
        const val EVENT_PROVIDER_FAILED = "provider_failed"
        const val EVENT_CACHE_HIT = "cache_hit"
        const val EVENT_CACHE_MISS = "cache_miss"
        const val EVENT_REQUEST_COMPLETED = "request_completed"
    }

    /**
     * Track when a provider is selected for an operation
     */
    fun trackProviderSelected(provider: Provider, operation: OperationType) {
        logEvent(
            eventName = EVENT_PROVIDER_SELECTED,
            params = mapOf(
                "provider" to provider.name,
                "operation" to operation.name,
            )
        )
        incrementDailyCounter("provider_selections_${provider.name}")
        incrementDailyCounter("${operation.name.lowercase()}_requests")
        incrementDailyCounter("total_requests")
    }

    /**
     * Track when a provider fails
     */
    fun trackProviderFailed(provider: Provider, operation: OperationType, error: String? = null) {
        logEvent(
            eventName = EVENT_PROVIDER_FAILED,
            params = mapOf(
                "provider" to provider.name,
                "operation" to operation.name,
                "error" to (error ?: "unknown"),
            )
        )
        incrementDailyCounter("provider_failures_${provider.name}")
        incrementDailyCounter("${operation.name.lowercase()}_failures")
        incrementDailyCounter("total_failures")
    }

    /**
     * Track cache hit
     */
    fun trackCacheHit(operation: OperationType) {
        logEvent(
            eventName = EVENT_CACHE_HIT,
            params = mapOf(
                "operation" to operation.name,
            )
        )
        incrementDailyCounter("cache_hits_${operation.name.lowercase()}")
        incrementDailyCounter("cache_hits_total")
    }

    /**
     * Track cache miss
     */
    fun trackCacheMiss(operation: OperationType) {
        logEvent(
            eventName = EVENT_CACHE_MISS,
            params = mapOf(
                "operation" to operation.name,
            )
        )
        incrementDailyCounter("cache_misses_${operation.name.lowercase()}")
        incrementDailyCounter("cache_misses_total")
    }

    /**
     * Track request completion with latency
     */
    fun trackRequestCompleted(operation: OperationType, latencyMs: Long, provider: Provider) {
        logEvent(
            eventName = EVENT_REQUEST_COMPLETED,
            params = mapOf(
                "operation" to operation.name,
                "provider" to provider.name,
                "latency_ms" to latencyMs,
            )
        )
        // Update latency sum for average calculation
        incrementDailyCounter("latency_sum_${operation.name.lowercase()}", latencyMs)
        incrementDailyCounter("latency_count_${operation.name.lowercase()}")
    }

    /**
     * Log an event to Firestore
     */
    private fun logEvent(eventName: String, params: Map<String, Any>) {
        scope.launch {
            try {
                val today = LocalDate.now(ZoneOffset.UTC).format(dateFormatter)
                val timestamp = Instant.now().toEpochMilli()

                val eventData = mapOf(
                    "event_name" to eventName,
                    "timestamp" to timestamp,
                    "date" to today,
                    "params" to params,
                )

                firestore
                    .collection(COLLECTION_ANALYTICS)
                    .document(today)
                    .collection(SUBCOLLECTION_EVENTS)
                    .add(eventData)
                    .get()

                logger.debug("Logged analytics event: $eventName with params: $params")
            } catch (e: Exception) {
                logger.error("Failed to log analytics event: $eventName", e)
            }
        }
    }

    /**
     * Increment a daily counter in the aggregates document
     */
    private fun incrementDailyCounter(counterName: String, incrementBy: Long = 1) {
        scope.launch {
            try {
                val today = LocalDate.now(ZoneOffset.UTC).format(dateFormatter)

                val docRef = firestore
                    .collection(COLLECTION_ANALYTICS)
                    .document(today)
                    .collection(SUBCOLLECTION_AGGREGATES)
                    .document(DOC_SUMMARY)

                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef).get()

                    val currentValue = if (snapshot.exists()) {
                        snapshot.getLong(counterName) ?: 0L
                    } else {
                        0L
                    }

                    val updates = mutableMapOf<String, Any>(
                        counterName to (currentValue + incrementBy),
                        "last_updated" to Instant.now().toEpochMilli(),
                        "date" to today,
                    )

                    if (snapshot.exists()) {
                        transaction.update(docRef, updates)
                    } else {
                        transaction.set(docRef, updates)
                    }

                    null
                }.get()
            } catch (e: Exception) {
                logger.error("Failed to increment counter: $counterName", e)
            }
        }
    }

    /**
     * Get daily analytics summary for a specific date
     */
    suspend fun getDailySummary(date: LocalDate = LocalDate.now(ZoneOffset.UTC)): Map<String, Any>? {
        return try {
            val dateStr = date.format(dateFormatter)
            val docRef = firestore
                .collection(COLLECTION_ANALYTICS)
                .document(dateStr)
                .collection(SUBCOLLECTION_AGGREGATES)
                .document(DOC_SUMMARY)

            val snapshot = docRef.get().get()

            if (snapshot.exists()) {
                snapshot.data?.toMutableMap()?.apply {
                    // Calculate cache hit rates
                    val searchHits = (get("cache_hits_search") as? Long) ?: 0L
                    val searchMisses = (get("cache_misses_search") as? Long) ?: 0L
                    val newsHits = (get("cache_hits_news") as? Long) ?: 0L
                    val newsMisses = (get("cache_misses_news") as? Long) ?: 0L
                    val infoHits = (get("cache_hits_info") as? Long) ?: 0L
                    val infoMisses = (get("cache_misses_info") as? Long) ?: 0L

                    put("search_cache_hit_rate", calculateHitRate(searchHits, searchMisses))
                    put("news_cache_hit_rate", calculateHitRate(newsHits, newsMisses))
                    put("info_cache_hit_rate", calculateHitRate(infoHits, infoMisses))

                    val totalHits = (get("cache_hits_total") as? Long) ?: 0L
                    val totalMisses = (get("cache_misses_total") as? Long) ?: 0L
                    put("overall_cache_hit_rate", calculateHitRate(totalHits, totalMisses))

                    // Calculate average latencies
                    listOf("search", "news", "info").forEach { op ->
                        val sum = (get("latency_sum_$op") as? Long) ?: 0L
                        val count = (get("latency_count_$op") as? Long) ?: 0L
                        put("avg_latency_${op}_ms", if (count > 0) sum.toDouble() / count else 0.0)
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get daily summary", e)
            null
        }
    }

    /**
     * Get analytics summary for date range
     */
    suspend fun getAnalyticsSummary(
        startDate: LocalDate = LocalDate.now(ZoneOffset.UTC).minusDays(7),
        endDate: LocalDate = LocalDate.now(ZoneOffset.UTC)
    ): List<Map<String, Any>> {
        val summaries = mutableListOf<Map<String, Any>>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            getDailySummary(currentDate)?.let { summary ->
                summaries.add(summary)
            }
            currentDate = currentDate.plusDays(1)
        }

        return summaries
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

