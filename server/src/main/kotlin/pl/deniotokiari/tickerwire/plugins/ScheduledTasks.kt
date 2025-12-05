package pl.deniotokiari.tickerwire.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import org.koin.ktor.ext.inject
import pl.deniotokiari.tickerwire.di.CacheQualifiers
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.services.cache.CacheCleanupScheduler
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheService

/**
 * Configure startup tasks for the application.
 * Runs cache cleanup once on server startup - optimized for Cloud Run.
 */
fun Application.configureScheduledTasks() {
    val cacheCleanupScheduler by inject<CacheCleanupScheduler>()

    // Eagerly load caches to trigger their registration with the cleanup scheduler
    // (Koin singletons are lazy by default, so we need to access them to create them)
    val searchCache by inject<FirestoreCacheService<List<TickerDto>>>(CacheQualifiers.SEARCH_CACHE)
    val newsCache by inject<FirestoreCacheService<List<TickerNewsDto>>>(CacheQualifiers.NEWS_CACHE)
    val infoCache by inject<FirestoreCacheService<TickerInfoDto>>(CacheQualifiers.INFO_CACHE)

    // Force cache creation by accessing them
    searchCache.hashCode()
    newsCache.hashCode()
    infoCache.hashCode()

    // Now run cleanup - caches are registered
    cacheCleanupScheduler.runStartupCleanup()

    // Cleanup on shutdown
    monitor.subscribe(ApplicationStopping) {
        cacheCleanupScheduler.shutdown()
    }
}
