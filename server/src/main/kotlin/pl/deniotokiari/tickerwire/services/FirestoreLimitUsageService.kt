package pl.deniotokiari.tickerwire.services

import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.LimitUsage
import pl.deniotokiari.tickerwire.models.Provider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for managing provider limit usage state in Firestore
 * Uses Firestore transactions to ensure atomic read-check-update operations
 */
class FirestoreLimitUsageService(private val firestore: Firestore) {
    private val collection = "provider_limits"
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Get current usage for a provider
     */
    suspend fun getUsage(provider: Provider): LimitUsage = withContext(Dispatchers.IO) {
        val doc = firestore.collection(collection)
            .document(provider.name)
            .get()
            .get()

        if (!doc.exists()) {
            return@withContext LimitUsage()
        }

        LimitUsage(
            lastUsed = doc.getString("last_used")?.let { LocalDateTime.parse(it, formatter) },
            usedCount = doc.getLong("used_count")?.toInt() ?: 0
        )
    }

    /**
     * Check if can use and atomically increment if yes
     * Returns the new usage if successful, null if limit exceeded
     * 
     * Uses Firestore transaction to prevent race conditions
     */
    suspend fun tryIncrementUsage(
        provider: Provider,
        config: LimitConfig
    ): LimitUsage? = withContext(Dispatchers.IO) {
        val docRef = firestore.collection(collection).document(provider.name)
        val now = LocalDateTime.now()

        // Use transaction for atomic read-check-update
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

            transaction.set(docRef, mapOf(
                "last_used" to now.format(formatter),
                "used_count" to newUsage.usedCount,
                "updated_at" to com.google.cloud.Timestamp.now()
            ))

            newUsage
        }.get()
    }

    /**
     * Increment usage without checking limits (use with caution)
     * Useful when you've already checked limits elsewhere
     */
    suspend fun incrementUsage(
        provider: Provider,
        config: LimitConfig
    ): LimitUsage = withContext(Dispatchers.IO) {
        val docRef = firestore.collection(collection).document(provider.name)
        val now = LocalDateTime.now()

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

            val newUsage = currentUsage.increment(config, now)

            transaction.set(docRef, mapOf(
                "last_used" to now.format(formatter),
                "used_count" to newUsage.usedCount,
                "updated_at" to com.google.cloud.Timestamp.now()
            ))

            newUsage
        }.get()
    }

    /**
     * Reset usage for a provider (for testing or manual reset)
     */
    suspend fun resetUsage(provider: Provider) = withContext(Dispatchers.IO) {
        firestore.collection(collection)
            .document(provider.name)
            .delete()
            .get()
    }

    /**
     * Reset usage for all providers
     */
    suspend fun resetAllUsage() = withContext(Dispatchers.IO) {
        val batch = firestore.batch()
        
        firestore.collection(collection)
            .listDocuments()
            .forEach { docRef ->
                batch.delete(docRef)
            }
        
        batch.commit().get()
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

