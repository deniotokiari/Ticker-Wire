package pl.deniotokiari.tickerwire.services.analytics

import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        private const val MAX_TRANSACTION_RETRIES = 5
        private const val INITIAL_RETRY_DELAY_MS = 10L
    }

    /**
     * Execute a transaction operation with explicit retry handling and exponential backoff
     * Handles distributed concurrency by retrying on transaction conflicts
     */
    private suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        operationName: String
    ): T {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < MAX_TRANSACTION_RETRIES) {
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                attempt++

                if (attempt < MAX_TRANSACTION_RETRIES) {
                    val backoffMs =
                        INITIAL_RETRY_DELAY_MS * (1 shl (attempt - 1)) // Exponential backoff
                    logger.debug(
                        "Transaction retry $attempt/$MAX_TRANSACTION_RETRIES for $operationName, " +
                                "backing off for ${backoffMs}ms",
                        e
                    )
                    delay(backoffMs)
                } else {
                    logger.error(
                        "Transaction failed after $MAX_TRANSACTION_RETRIES retries for $operationName",
                        e
                    )
                }
            }
        }

        throw lastException
            ?: Exception("Transaction failed after $MAX_TRANSACTION_RETRIES attempts: $operationName")
    }

    private fun getCurrentMonth(): String {
        return YearMonth.now(ZoneOffset.UTC).format(monthFormatter)
    }

    /**
     * Increment provider selection count
     * Includes explicit retry handling for distributed concurrency
     */
    suspend fun recordSelection(provider: Provider) = withContext(Dispatchers.IO) {
        try {
            val month = getCurrentMonth()
            val docRef = firestore
                .collection(COLLECTION_STATS)
                .document(month)
                .collection(SUBCOLLECTION_PROVIDERS)
                .document(provider.name)

            executeWithRetry(
                operation = {
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

                        // Use server timestamp for better consistency
                        data["updated_at"] = FieldValue.serverTimestamp()

                        if (snapshot.exists()) {
                            transaction.update(docRef, data)
                        } else {
                            transaction.set(docRef, data)
                        }

                        null
                    }.get()
                },
                operationName = "recordSelection(${provider.name})"
            )
        } catch (e: Exception) {
            logger.error("Failed to record selection for provider: ${provider.name}", e)
        }
    }

    /**
     * Increment provider failure count
     * Includes explicit retry handling for distributed concurrency
     */
    suspend fun recordFailure(provider: Provider) = withContext(Dispatchers.IO) {
        try {
            val month = getCurrentMonth()
            val docRef = firestore
                .collection(COLLECTION_STATS)
                .document(month)
                .collection(SUBCOLLECTION_PROVIDERS)
                .document(provider.name)

            executeWithRetry(
                operation = {
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

                        // Use server timestamp for better consistency
                        data["updated_at"] = FieldValue.serverTimestamp()

                        if (snapshot.exists()) {
                            transaction.update(docRef, data)
                        } else {
                            transaction.set(docRef, data)
                        }

                        null
                    }.get()
                },
                operationName = "recordFailure(${provider.name})"
            )
        } catch (e: Exception) {
            logger.error("Failed to record failure for provider: ${provider.name}", e)
        }
    }

    /**
     * Record multiple selections and failures in batch using Firestore transactions
     * More efficient than individual recordSelection/recordFailure calls
     *
     * Uses transactions to ensure atomic read-modify-write operations
     * Handles Firestore's 500 operation limit by chunking if needed
     *
     * Note: Firestore transactions have a limit of 500 documents, so we chunk providers
     * if needed. Each chunk is processed in a separate transaction to ensure atomicity.
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

            // Process in chunks to respect Firestore transaction limit (500 documents)
            // Each chunk is processed atomically in a single transaction with retry handling
            // IMPORTANT: Firestore requires all reads before all writes in a transaction
            val chunkSize = 500
            allProviders.chunked(chunkSize).forEach { providerChunk ->
                executeWithRetry(
                    operation = {
                        firestore.runTransaction { transaction ->
                            // Step 1: Create all document references
                            val docRefs = providerChunk.map { provider ->
                                provider to firestore
                                    .collection(COLLECTION_STATS)
                                    .document(month)
                                    .collection(SUBCOLLECTION_PROVIDERS)
                                    .document(provider.name)
                            }
                            
                            // Step 2: Execute ALL reads first (Firestore requirement)
                            val readResults = docRefs.map { (provider, docRef) ->
                                val doc = transaction.get(docRef).get()
                                val currentSelections = if (doc.exists()) {
                                    doc.getLong(FIELD_SELECTIONS) ?: 0L
                                } else {
                                    0L
                                }
                                val currentFailures = if (doc.exists()) {
                                    doc.getLong(FIELD_FAILURES) ?: 0L
                                } else {
                                    0L
                                }
                                val docExists = doc.exists()
                                
                                provider to Triple(currentSelections, currentFailures, docExists)
                            }.toMap()
                            
                            // Step 3: Execute ALL writes after reads
                            docRefs.forEach { (provider, docRef) ->
                                val (currentSelections, currentFailures, docExists) = readResults[provider]!!
                                val selectionCount = selections[provider] ?: 0
                                val failureCount = failures[provider] ?: 0
                                
                                val newSelections = currentSelections + selectionCount
                                val newFailures = currentFailures + failureCount
                                
                                val data = mutableMapOf<String, Any>(
                                    FIELD_SELECTIONS to newSelections,
                                    FIELD_FAILURES to newFailures,
                                    "updated_at" to FieldValue.serverTimestamp()
                                )
                                
                                if (docExists) {
                                    transaction.update(docRef, data)
                                } else {
                                    transaction.set(docRef, data)
                                }
                            }
                            
                            null
                        }.get()
                    },
                    operationName = "recordBatch(chunk of ${providerChunk.size} providers)"
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to record batch stats", e)
            // Fallback to individual calls if batch fails
            selections.forEach { (provider, count) ->
                repeat(count) {
                    try {
                        recordSelection(provider)
                    } catch (ex: Exception) {
                        logger.error(
                            "Failed to record selection for provider: ${provider.name}",
                            ex
                        )
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

