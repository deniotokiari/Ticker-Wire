package pl.deniotokiari.tickerwire.services

import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.LimitUsage
import pl.deniotokiari.tickerwire.models.Provider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for managing provider limit usage state in Firestore
 * Uses Firestore transactions to ensure atomic read-check-update operations
 * 
 * Optimizations for scale:
 * - Batch reads using Firestore getAll API
 * - In-memory cache with short TTL (1 second) to reduce Firestore reads
 * - Cache invalidation on writes to ensure consistency across instances
 * - All writes remain transactional for correctness
 */
class FirestoreLimitUsageService(private val firestore: Firestore) {
    private val logger = LoggerFactory.getLogger(FirestoreLimitUsageService::class.java)
    private val collection = "provider_limits"
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    // In-memory cache: Provider -> (Usage, timestamp)
    // Short TTL (1 second) to balance performance and consistency across instances
    private val usageCache = mutableMapOf<Provider, Pair<LimitUsage, Long>>()
    private val cacheMutex = Mutex()
    private val cacheTtlMs = 1000L // 1 second - short enough to minimize staleness
    
    companion object {
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
                
                // Check if it's a transaction conflict (Firestore retries automatically, but we log it)
                if (attempt < MAX_TRANSACTION_RETRIES) {
                    val backoffMs = INITIAL_RETRY_DELAY_MS * (1 shl (attempt - 1)) // Exponential backoff
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
        
        throw lastException ?: Exception("Transaction failed after $MAX_TRANSACTION_RETRIES attempts: $operationName")
    }

    /**
     * Get current usage for a provider
     * Uses in-memory cache with short TTL to reduce Firestore reads
     */
    suspend fun getUsage(provider: Provider): LimitUsage = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Check cache first
        cacheMutex.withLock {
            val cached = usageCache[provider]
            if (cached != null && (now - cached.second) < cacheTtlMs) {
                return@withContext cached.first
            }
        }
        
        // Cache miss or expired - fetch from Firestore
        val doc = firestore.collection(collection)
            .document(provider.name)
            .get()
            .get()

        val usage = if (!doc.exists()) {
            LimitUsage()
        } else {
            LimitUsage(
                lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
                usedCount = doc.getLong("used_count")?.toInt() ?: 0
            )
        }
        
        // Update cache
        cacheMutex.withLock {
            usageCache[provider] = usage to now
        }
        
        usage
    }
    
    /**
     * Get usage for multiple providers using Firestore batch getAll API
     * More efficient than individual getUsage calls
     * 
     * @param providers Collection of providers to fetch usage for
     * @return Map of provider to usage
     */
    suspend fun getUsagesBatch(providers: Collection<Provider>): Map<Provider, LimitUsage> = withContext(Dispatchers.IO) {
        if (providers.isEmpty()) {
            return@withContext emptyMap()
        }
        
        val now = System.currentTimeMillis()
        val result = mutableMapOf<Provider, LimitUsage>()
        val providersToFetch = mutableSetOf<Provider>()
        
        // Check cache first
        cacheMutex.withLock {
            providers.forEach { provider ->
                val cached = usageCache[provider]
                if (cached != null && (now - cached.second) < cacheTtlMs) {
                    result[provider] = cached.first
                } else {
                    providersToFetch.add(provider)
                }
            }
        }
        
        // Batch fetch remaining providers from Firestore
        if (providersToFetch.isNotEmpty()) {
            val batchSize = 500 // Firestore batch limit
            
            providersToFetch.chunked(batchSize).forEach { batch ->
                val docRefs = batch.map { provider ->
                    provider to firestore.collection(collection).document(provider.name)
                }
                
                val documents = firestore.getAll(*docRefs.map { it.second }.toTypedArray()).get()
                
                docRefs.forEachIndexed { index, (provider, _) ->
                    val doc = documents[index]
                    val usage = if (doc.exists()) {
                        LimitUsage(
                            lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
                            usedCount = doc.getLong("used_count")?.toInt() ?: 0
                        )
                    } else {
                        LimitUsage()
                    }
                    
                    result[provider] = usage
                    
                    // Update cache
                    cacheMutex.withLock {
                        usageCache[provider] = usage to now
                    }
                }
            }
        }
        
        result
    }

    /**
     * Check if can use and atomically increment if yes
     * Returns the new usage if successful, null if limit exceeded
     * 
     * Uses Firestore transaction to prevent race conditions across multiple instances
     * Includes explicit retry handling with exponential backoff for distributed concurrency
     * Invalidates cache after successful increment to ensure consistency
     */
    suspend fun tryIncrementUsage(
        provider: Provider,
        config: LimitConfig
    ): LimitUsage? = withContext(Dispatchers.IO) {
        val docRef = firestore.collection(collection).document(provider.name)
        val now = LocalDateTime.now()

        // Use transaction with explicit retry handling for distributed concurrency
        // Firestore automatically retries, but we add explicit handling and logging
        val result = executeWithRetry(
            operation = {
                firestore.runTransaction { transaction ->
                val doc = transaction.get(docRef).get()

                val currentUsage = if (doc.exists()) {
                    LimitUsage(
                        lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
                        usedCount = doc.getLong("used_count")?.toInt() ?: 0
                    )
                } else {
                    LimitUsage()
                }

                // Check if we can use (with reset logic applied)
                if (!currentUsage.canUse(config, now)) {
                    return@runTransaction null
                }

                // Increment and save
                val newUsage = currentUsage.increment(config, now)

                // Use server timestamp for better consistency across instances
                transaction.set(docRef, mapOf(
                    "last_used" to now.format(formatter),
                    "used_count" to newUsage.usedCount,
                    "updated_at" to FieldValue.serverTimestamp()
                ))

                newUsage
            }.get()
            },
            operationName = "tryIncrementUsage(${provider.name})"
        )
        
        // Invalidate cache after successful increment
        // This ensures other instances see updated usage (after their cache expires)
        if (result != null) {
            cacheMutex.withLock {
                usageCache.remove(provider)
            }
        }
        
        result
    }

    /**
     * Increment usage without checking limits (use with caution)
     * Useful when you've already checked limits elsewhere
     * Includes explicit retry handling for distributed concurrency
     * Invalidates cache after increment to ensure consistency
     */
    suspend fun incrementUsage(
        provider: Provider,
        config: LimitConfig
    ): LimitUsage = withContext(Dispatchers.IO) {
        val docRef = firestore.collection(collection).document(provider.name)
        val now = LocalDateTime.now()

        val newUsage = executeWithRetry<LimitUsage>(
            operation = {
                firestore.runTransaction { transaction ->
                val doc = transaction.get(docRef).get()

                val currentUsage = if (doc.exists()) {
                    LimitUsage(
                        lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
                        usedCount = doc.getLong("used_count")?.toInt() ?: 0
                    )
                } else {
                    LimitUsage()
                }

                val usage = currentUsage.increment(config, now)

                // Use server timestamp for better consistency across instances
                transaction.set(docRef, mapOf(
                    "last_used" to now.format(formatter),
                    "used_count" to usage.usedCount,
                    "updated_at" to FieldValue.serverTimestamp()
                ))

                usage
            }.get()
            },
            operationName = "incrementUsage(${provider.name})"
        )
        
        // Invalidate cache after increment
        cacheMutex.withLock {
            usageCache.remove(provider)
        }
        
        newUsage
    }

    /**
     * Reset usage for a provider (for testing or manual reset)
     * Invalidates cache after reset
     */
    suspend fun resetUsage(provider: Provider) = withContext(Dispatchers.IO) {
        firestore.collection(collection)
            .document(provider.name)
            .delete()
            .get()
        
        // Invalidate cache
        cacheMutex.withLock {
            usageCache.remove(provider)
        }
    }

    /**
     * Reset usage for all providers
     * Invalidates cache after reset
     */
    suspend fun resetAllUsage() = withContext(Dispatchers.IO) {
        val batch = firestore.batch()
        
        firestore.collection(collection)
            .listDocuments()
            .forEach { docRef ->
                batch.delete(docRef)
            }
        
        batch.commit().get()
        
        // Invalidate entire cache after reset
        cacheMutex.withLock {
            usageCache.clear()
        }
    }

    /**
     * Get usage for all providers
     */
    suspend fun getAllUsage(): Map<Provider, LimitUsage> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Provider, LimitUsage>()
        
        firestore.collection(collection)
            .get()
            .get()
            .documents
            .forEach { doc ->
                try {
                    val provider = Provider.valueOf(doc.id)
                    val usage = LimitUsage(
                        lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
                        usedCount = doc.getLong("used_count")?.toInt() ?: 0
                    )
                    result[provider] = usage
                } catch (e: IllegalArgumentException) {
                    // Unknown provider, skip
                }
            }
        
        result
    }
}

