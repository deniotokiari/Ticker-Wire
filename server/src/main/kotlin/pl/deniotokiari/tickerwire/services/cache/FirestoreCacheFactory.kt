package pl.deniotokiari.tickerwire.services.cache

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import kotlinx.serialization.KSerializer
import java.time.Duration

/**
 * Factory for creating FirestoreCacheService instances with common configurations
 */
class FirestoreCacheFactory(
    private val firestore: Firestore = FirestoreClient.getFirestore(),
) {

    /**
     * Create a cache for search results
     */
    fun <T : Any> searchCache(
        name: String,
        serializer: KSerializer<T>,
        ttl: Duration = Duration.ofHours(1),
    ): FirestoreCacheService<T> = FirestoreCacheService(
        name = name,
        serializer = serializer,
        ttl = ttl,
        keyGenerator = { query -> query.toString().lowercase().trim() },
        firestore = firestore,
    )

    /**
     * Create a cache for news results
     */
    fun <T : Any> newsCache(
        name: String,
        serializer: KSerializer<T>,
        ttl: Duration = Duration.ofMinutes(15),
    ): FirestoreCacheService<T> = FirestoreCacheService(
        name = name,
        serializer = serializer,
        ttl = ttl,
        keyGenerator = { tickers ->
            when (tickers) {
                is List<*> -> "news_${tickers.sortedBy { it.toString() }.joinToString("_")}"
                else -> "news_$tickers"
            }
        },
        firestore = firestore,
    )

    /**
     * Create a cache for ticker info results
     */
    fun <T : Any> infoCache(
        name: String,
        serializer: KSerializer<T>,
        ttl: Duration = Duration.ofMinutes(5),
    ): FirestoreCacheService<T> = FirestoreCacheService(
        name = name,
        serializer = serializer,
        ttl = ttl,
        keyGenerator = { tickers ->
            when (tickers) {
                is List<*> -> "info_${tickers.sortedBy { it.toString() }.joinToString("_")}"
                else -> "info_$tickers"
            }
        },
        firestore = firestore,
    )

    /**
     * Create a custom cache with specified parameters
     */
    fun <T : Any> custom(
        name: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        keyGenerator: (Any) -> String,
    ): FirestoreCacheService<T> = FirestoreCacheService(
        name = name,
        serializer = serializer,
        ttl = ttl,
        keyGenerator = keyGenerator,
        firestore = firestore,
    )
}

/**
 * DSL builder for creating FirestoreCacheService
 */
class FirestoreCacheBuilder<T : Any>(
    private val firestore: Firestore = FirestoreClient.getFirestore(),
) {
    lateinit var name: String
    lateinit var serializer: KSerializer<T>
    var ttl: Duration = Duration.ofMinutes(30)
    var keyGenerator: (Any) -> String = { it.toString() }

    fun build(): FirestoreCacheService<T> {
        require(::name.isInitialized) { "Cache name must be specified" }
        require(::serializer.isInitialized) { "Serializer must be specified" }

        return FirestoreCacheService(
            name = name,
            serializer = serializer,
            ttl = ttl,
            keyGenerator = keyGenerator,
            firestore = firestore,
        )
    }
}

/**
 * DSL function to create FirestoreCacheService
 *
 * Example:
 * ```
 * val cache = firestoreCache<List<TickerDto>> {
 *     name = "search_cache"
 *     serializer = ListSerializer(TickerDto.serializer())
 *     ttl = Duration.ofHours(1)
 *     keyGenerator = { query -> "search_${query.toString().lowercase()}" }
 * }
 * ```
 */
fun <T : Any> firestoreCache(
    firestore: Firestore = FirestoreClient.getFirestore(),
    block: FirestoreCacheBuilder<T>.() -> Unit,
): FirestoreCacheService<T> {
    return FirestoreCacheBuilder<T>(firestore).apply(block).build()
}

