package pl.deniotokiari.tickerwire.services.cache

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant

/**
 * Firestore-based cache service with configurable TTL and key generation
 *
 * @param T The type of data to cache
 * @param name The Firestore collection name for this cache
 * @param serializer The kotlinx.serialization serializer for the cached type
 * @param ttl Time-to-live duration for cache entries
 * @param keyGenerator Function to generate cache keys from input parameters
 * @param firestore The Firestore instance (defaults to FirestoreClient.getFirestore())
 */
class FirestoreCacheService<T : Any>(
    private val name: String,
    private val serializer: KSerializer<T>,
    private val ttl: Duration,
    private val keyGenerator: (Any) -> String = { it.toString() },
    private val firestore: Firestore = FirestoreClient.getFirestore(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get cached value or fetch from source if not cached/expired
     *
     * @param key The cache key input (will be processed by keyGenerator)
     * @param fetch The suspend function to fetch data if cache miss
     * @return The cached or freshly fetched data
     */
    suspend fun getOrFetch(
        key: Any,
        fetch: suspend () -> T,
    ): T {
        val cacheKey = keyGenerator(key)
        
        // Try to get from cache
        val cached = get(cacheKey)
        if (cached != null) {
            return cached
        }
        
        // Fetch and cache
        val data = fetch()
        put(cacheKey, data)
        return data
    }

    /**
     * Get cached value if exists and not expired
     *
     * @param key The raw cache key (not processed by keyGenerator)
     * @return The cached data or null if not found/expired
     */
    suspend fun get(key: String): T? = withContext(Dispatchers.IO) {
        val doc = firestore.collection(name)
            .document(sanitizeKey(key))
            .get()
            .get()

        if (!doc.exists()) {
            return@withContext null
        }

        val expiresAt = doc.getTimestamp("expires_at")
        if (expiresAt != null && Timestamp.now().toDate().after(expiresAt.toDate())) {
            // Entry expired, delete it
            delete(key)
            return@withContext null
        }

        val dataJson = doc.getString("data") ?: return@withContext null
        
        try {
            json.decodeFromString(serializer, dataJson)
        } catch (e: Exception) {
            // Corrupted data, delete it
            delete(key)
            null
        }
    }

    /**
     * Store value in cache with TTL
     *
     * @param key The raw cache key (not processed by keyGenerator)
     * @param value The value to cache
     */
    suspend fun put(key: String, value: T) = withContext(Dispatchers.IO) {
        val dataJson = json.encodeToString(serializer, value)
        val now = Instant.now()
        val expiresAt = now.plus(ttl)

        firestore.collection(name)
            .document(sanitizeKey(key))
            .set(mapOf(
                "data" to dataJson,
                "created_at" to Timestamp.ofTimeSecondsAndNanos(now.epochSecond, now.nano),
                "expires_at" to Timestamp.ofTimeSecondsAndNanos(expiresAt.epochSecond, expiresAt.nano),
                "ttl_seconds" to ttl.seconds,
            ))
            .get()
    }

    /**
     * Delete cached entry
     *
     * @param key The raw cache key (not processed by keyGenerator)
     */
    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        firestore.collection(name)
            .document(sanitizeKey(key))
            .delete()
            .get()
    }

    /**
     * Delete all entries in this cache
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        val batch = firestore.batch()
        
        firestore.collection(name)
            .listDocuments()
            .forEach { docRef ->
                batch.delete(docRef)
            }
        
        batch.commit().get()
    }

    /**
     * Delete all expired entries
     */
    suspend fun cleanupExpired() = withContext(Dispatchers.IO) {
        val now = Timestamp.now()
        val batch = firestore.batch()
        
        firestore.collection(name)
            .whereLessThan("expires_at", now)
            .get()
            .get()
            .documents
            .forEach { doc ->
                batch.delete(doc.reference)
            }
        
        batch.commit().get()
    }

    /**
     * Check if a key exists and is not expired
     *
     * @param key The raw cache key (not processed by keyGenerator)
     */
    suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        val doc = firestore.collection(name)
            .document(sanitizeKey(key))
            .get()
            .get()

        if (!doc.exists()) {
            return@withContext false
        }

        val expiresAt = doc.getTimestamp("expires_at")
        expiresAt == null || !Timestamp.now().toDate().after(expiresAt.toDate())
    }

    /**
     * Update TTL for an existing entry without changing the data
     *
     * @param key The raw cache key (not processed by keyGenerator)
     * @return true if entry was updated, false if entry doesn't exist
     */
    suspend fun touch(key: String): Boolean = withContext(Dispatchers.IO) {
        val docRef = firestore.collection(name).document(sanitizeKey(key))
        val doc = docRef.get().get()

        if (!doc.exists()) {
            return@withContext false
        }

        val now = Instant.now()
        val expiresAt = now.plus(ttl)

        docRef.update(mapOf(
            "expires_at" to Timestamp.ofTimeSecondsAndNanos(expiresAt.epochSecond, expiresAt.nano),
        )).get()

        true
    }

    /**
     * Sanitize key to be a valid Firestore document ID
     * Firestore document IDs cannot contain '/'
     */
    private fun sanitizeKey(key: String): String {
        return key.replace("/", "_")
            .replace("\\", "_")
            .take(1500) // Firestore document ID limit
    }
}

