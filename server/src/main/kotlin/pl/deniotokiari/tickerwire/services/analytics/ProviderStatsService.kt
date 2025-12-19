package pl.deniotokiari.tickerwire.services.analytics

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import pl.deniotokiari.tickerwire.models.Provider
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Simple provider statistics service
 * Tracks selection and failure counts per provider, organized by month
 *
 * Firestore structure:
 * provider_stats/{year-month}/providers/{PROVIDER_NAME}
 *   - selections: Long
 *   - failures: Long
 */
class ProviderStatsService(
    private val firestore: Firestore,
) {
    private val logger = LoggerFactory.getLogger(ProviderStatsService::class.java)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    companion object {
        private const val COLLECTION_STATS = "provider_stats"
        private const val SUBCOLLECTION_PROVIDERS = "providers"
        private const val FIELD_SELECTIONS = "selections"
        private const val FIELD_FAILURES = "failures"
    }

    private fun getCurrentMonth(): String {
        return YearMonth.now(ZoneOffset.UTC).format(monthFormatter)
    }

    /**
     * Increment provider selection count
     */
    suspend fun recordSelection(provider: Provider) = withContext(Dispatchers.IO) {
        try {
            val month = getCurrentMonth()
            val docRef = firestore
                .collection(COLLECTION_STATS)
                .document(month)
                .collection(SUBCOLLECTION_PROVIDERS)
                .document(provider.name)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef).get()
                val currentSelections = if (snapshot.exists()) {
                    snapshot.getLong(FIELD_SELECTIONS) ?: 0L
                } else {
                    0L
                }

                val data = mutableMapOf<String, Any>(
                    FIELD_SELECTIONS to (currentSelections + 1)
                )

                if (!snapshot.exists()) {
                    data[FIELD_FAILURES] = 0L
                }

                if (snapshot.exists()) {
                    transaction.update(docRef, data)
                } else {
                    transaction.set(docRef, data)
                }

                null
            }.get()
        } catch (e: Exception) {
            logger.error("Failed to record selection for provider: ${provider.name}", e)
        }
    }

    /**
     * Increment provider failure count
     */
    suspend fun recordFailure(provider: Provider) = withContext(Dispatchers.IO) {
        try {
            val month = getCurrentMonth()
            val docRef = firestore
                .collection(COLLECTION_STATS)
                .document(month)
                .collection(SUBCOLLECTION_PROVIDERS)
                .document(provider.name)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef).get()
                val currentFailures = if (snapshot.exists()) {
                    snapshot.getLong(FIELD_FAILURES) ?: 0L
                } else {
                    0L
                }

                val data = mutableMapOf<String, Any>(
                    FIELD_FAILURES to (currentFailures + 1)
                )

                if (!snapshot.exists()) {
                    data[FIELD_SELECTIONS] = 0L
                }

                if (snapshot.exists()) {
                    transaction.update(docRef, data)
                } else {
                    transaction.set(docRef, data)
                }

                null
            }.get()
        } catch (e: Exception) {
            logger.error("Failed to record failure for provider: ${provider.name}", e)
        }
    }

    /**
     * Record multiple selections and failures in batch using Firestore batch writes
     * More efficient than individual recordSelection/recordFailure calls
     * 
     * Uses batch getAll for reads and batch writes for updates
     * Handles Firestore's 500 operation limit by chunking if needed
     * 
     * @param selections Map of provider to count of selections
     * @param failures Map of provider to count of failures
     */
    suspend fun recordBatch(
        selections: Map<Provider, Int> = emptyMap(),
        failures: Map<Provider, Int> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (selections.isEmpty() && failures.isEmpty()) {
            return@withContext
        }

        try {
            val month = getCurrentMonth()
            val allProviders = (selections.keys + failures.keys).toSet()
            
            if (allProviders.isEmpty()) {
                return@withContext
            }

            // Create document references in a list to preserve order
            val providerList = allProviders.toList()
            val docRefs = providerList.map { provider ->
                provider to firestore
                    .collection(COLLECTION_STATS)
                    .document(month)
                    .collection(SUBCOLLECTION_PROVIDERS)
                    .document(provider.name)
            }
            
            // Batch read current stats
            val documents = firestore.getAll(*docRefs.map { it.second }.toTypedArray()).get()
            val currentStats = docRefs.mapIndexed { index, (provider, _) ->
                val doc = documents[index]
                provider to Pair(
                    if (doc.exists()) doc.getLong(FIELD_SELECTIONS) ?: 0L else 0L,
                    if (doc.exists()) doc.getLong(FIELD_FAILURES) ?: 0L else 0L
                )
            }.toMap()
            val docExistsMap = docRefs.mapIndexed { index, (provider, _) ->
                provider to documents[index].exists()
            }.toMap()
            
            // Prepare batch writes (handle 500 operation limit)
            val batchSize = 500
            providerList.chunked(batchSize).forEach { batchProviders ->
                val batch = firestore.batch()
                
                batchProviders.forEach { provider ->
                    val (_, docRef) = docRefs.first { it.first == provider }
                    val (currentSelections, currentFailures) = currentStats[provider] ?: Pair(0L, 0L)
                    val selectionCount = selections[provider] ?: 0
                    val failureCount = failures[provider] ?: 0
                    
                    val newSelections = currentSelections + selectionCount
                    val newFailures = currentFailures + failureCount
                    
                    val data = mutableMapOf<String, Any>(
                        FIELD_SELECTIONS to newSelections,
                        FIELD_FAILURES to newFailures
                    )
                    
                    if (docExistsMap[provider] == true) {
                        batch.update(docRef, data)
                    } else {
                        batch.set(docRef, data)
                    }
                }
                
                batch.commit().get()
            }
        } catch (e: Exception) {
            logger.error("Failed to record batch stats", e)
            // Fallback to individual calls if batch fails
            selections.forEach { (provider, count) ->
                repeat(count) {
                    try {
                        recordSelection(provider)
                    } catch (ex: Exception) {
                        logger.error("Failed to record selection for provider: ${provider.name}", ex)
                    }
                }
            }
            failures.forEach { (provider, count) ->
                repeat(count) {
                    try {
                        recordFailure(provider)
                    } catch (ex: Exception) {
                        logger.error("Failed to record failure for provider: ${provider.name}", ex)
                    }
                }
            }
        }
    }

    /**
     * Get stats for current month
     */
    suspend fun getCurrentMonthStats(): MonthlyStats = withContext(Dispatchers.IO) {
        getStatsForMonth(getCurrentMonth())
    }

    /**
     * Get stats for a specific month (format: yyyy-MM)
     */
    suspend fun getStatsForMonth(month: String): MonthlyStats = withContext(Dispatchers.IO) {
        try {
            val providerDocs = firestore
                .collection(COLLECTION_STATS)
                .document(month)
                .collection(SUBCOLLECTION_PROVIDERS)
                .get()
                .get()

            val providers = providerDocs.documents.associate { doc ->
                doc.id to ProviderStat(
                    provider = doc.id,
                    selections = doc.getLong(FIELD_SELECTIONS) ?: 0L,
                    failures = doc.getLong(FIELD_FAILURES) ?: 0L,
                )
            }

            MonthlyStats(
                month = month,
                providers = providers,
                totalSelections = providers.values.sumOf { it.selections },
                totalFailures = providers.values.sumOf { it.failures },
            )
        } catch (e: Exception) {
            logger.error("Failed to get stats for month: $month", e)
            MonthlyStats(month = month)
        }
    }
}

@Serializable
data class ProviderStat(
    val provider: String,
    val selections: Long = 0,
    val failures: Long = 0,
)

@Serializable
data class MonthlyStats(
    val month: String,
    val providers: Map<String, ProviderStat> = emptyMap(),
    val totalSelections: Long = 0,
    val totalFailures: Long = 0,
)

