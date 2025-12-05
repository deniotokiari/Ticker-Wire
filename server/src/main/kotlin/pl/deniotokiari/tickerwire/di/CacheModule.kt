package pl.deniotokiari.tickerwire.di

import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.services.cache.CacheCleanupScheduler
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheFactory
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheService

/**
 * Cache qualifiers for dependency injection
 */
object CacheQualifiers {
    val SEARCH_CACHE = named("search_cache")
    val NEWS_CACHE = named("news_cache")
    val INFO_CACHE = named("info_cache")
}

/**
 * Koin module for cache services.
 * Caches are created as singletons and registered with the cleanup scheduler.
 */
val cacheModule = module {
    // Search cache
    single<FirestoreCacheService<List<TickerDto>>>(CacheQualifiers.SEARCH_CACHE) {
        val cacheFactory = get<FirestoreCacheFactory>()
        val cache = cacheFactory.searchCache(
            name = "search_cache",
            serializer = ListSerializer(TickerDto.serializer()),
        )
        // Register with cleanup scheduler
        get<CacheCleanupScheduler>().register("search_cache", cache)
        cache
    }

    // News cache
    single<FirestoreCacheService<List<TickerNewsDto>>>(CacheQualifiers.NEWS_CACHE) {
        val cacheFactory = get<FirestoreCacheFactory>()
        val cache = cacheFactory.newsCache(
            name = "news_cache",
            serializer = ListSerializer(TickerNewsDto.serializer()),
        )
        // Register with cleanup scheduler
        get<CacheCleanupScheduler>().register("news_cache", cache)
        cache
    }

    // Info cache
    single<FirestoreCacheService<TickerInfoDto>>(CacheQualifiers.INFO_CACHE) {
        val cacheFactory = get<FirestoreCacheFactory>()
        val cache = cacheFactory.infoCache(
            name = "info_cache",
            serializer = TickerInfoDto.serializer(),
        )
        // Register with cleanup scheduler
        get<CacheCleanupScheduler>().register("info_cache", cache)
        cache
    }
}

