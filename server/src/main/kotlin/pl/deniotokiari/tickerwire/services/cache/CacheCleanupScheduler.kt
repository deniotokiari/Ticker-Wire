package pl.deniotokiari.tickerwire.services.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for cache cleanup operations.
 * Runs cleanup once on server startup - optimized for Cloud Run's scale-to-zero behavior.
 */
class CacheCleanupScheduler {
    private val logger = LoggerFactory.getLogger(CacheCleanupScheduler::class.java)
    private val caches = ConcurrentHashMap<String, FirestoreCacheService<*>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Register a cache for cleanup
     *
     * @param name Unique identifier for the cache
     * @param cache The cache service to register
     */
    fun register(name: String, cache: FirestoreCacheService<*>) {
        caches[name] = cache
        logger.info("Registered cache '$name' for cleanup")
    }

    /**
     * Unregister a cache from cleanup
     *
     * @param name The cache identifier to unregister
     */
    fun unregister(name: String) {
        caches.remove(name)
        logger.info("Unregistered cache '$name' from cleanup")
    }

    /**
     * Run cleanup once for all registered caches.
     * Called on server startup to clean expired entries.
     */
    fun runStartupCleanup() {
        if (caches.isEmpty()) {
            logger.info("No caches registered, skipping startup cleanup")
            return
        }

        logger.info("Starting startup cache cleanup for ${caches.size} registered cache(s)")

        scope.launch {
            var successCount = 0
            var errorCount = 0

            caches.forEach { (name, cache) ->
                try {
                    cache.cleanupExpired()
                    successCount++
                    logger.debug("Successfully cleaned up cache '$name'")
                } catch (e: Exception) {
                    errorCount++
                    logger.error("Failed to clean up cache '$name': ${e.message}", e)
                }
            }

            logger.info("Startup cache cleanup completed: $successCount successful, $errorCount failed")
        }
    }

    /**
     * Get the number of registered caches
     */
    fun registeredCacheCount(): Int = caches.size

    /**
     * Clear all registered caches (for shutdown)
     */
    fun shutdown() {
        caches.clear()
        logger.info("Cache cleanup manager shut down")
    }
}
