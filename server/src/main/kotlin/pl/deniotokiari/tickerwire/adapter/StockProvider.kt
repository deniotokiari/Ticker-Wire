package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.LimitUsage
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig
import pl.deniotokiari.tickerwire.plugins.NoAvailableProviderException
import pl.deniotokiari.tickerwire.services.FirestoreLimitUsageService
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheService
import java.time.LocalDateTime

/**
 * Aggregator for stock data providers
 * Handles provider selection based on priority ranking and availability
 *
 * Priority is determined by:
 * 1. Provider tier (high-frequency providers first)
 * 2. Remaining capacity within the same tier
 *
 * Provider Priority Rankings (based on limits and functionality):
 * - Tier 1 (High Frequency): FINNHUB (60/min), MASSIVE (5/min)
 * - Tier 2 (Medium Volume): FINANCIALMODELINGPREP (250/day), STOCKDATA (100/day), MARKETAUX (100/day)
 * - Tier 3 (Low Volume): ALPHAVANTAGE (25/day), MARKETSTACK (100/month)
 *
 * Configuration (API keys, limits) is stored in Firebase Remote Config
 * Usage state (lastUsed, usedCount) is stored in Firestore
 */
class StockProvider(
    private val providerConfigService: ProviderConfigService,
    private val limitUsageService: FirestoreLimitUsageService,
    private val newsProviders: Map<Provider, StockNewsProvider>,
    private val searchProviders: Map<Provider, StockSearchProvider>,
    private val infoProviders: Map<Provider, StockInfoProvider>,
    private val searchCache: FirestoreCacheService<List<TickerDto>>,
    private val newsCache: FirestoreCacheService<List<TickerNewsDto>>,
    private val infoCache: FirestoreCacheService<TickerInfoDto>,
    private val statsService: ProviderStatsService,
) {

    companion object {
        /**
         * Provider priority for SEARCH operations (lower = higher priority)
         * Order: FINNHUB → MASSIVE → FMP → STOCKDATA → MARKETAUX → ALPHAVANTAGE → MARKETSTACK
         */
        val SEARCH_PRIORITY = mapOf(
            Provider.FINNHUB to 1,
            Provider.MASSIVE to 2,
            Provider.FINANCIALMODELINGPREP to 3,
            Provider.STOCKDATA to 4,
            Provider.MARKETAUX to 5,
            Provider.ALPHAVANTAGE to 6,
            Provider.MARKETSTACK to 7,
        )

        /**
         * Provider priority for NEWS operations (lower = higher priority)
         * Order: FINNHUB → MASSIVE → STOCKDATA → MARKETAUX → ALPHAVANTAGE
         */
        val NEWS_PRIORITY = mapOf(
            Provider.FINNHUB to 1,
            Provider.MASSIVE to 2,
            Provider.STOCKDATA to 3,
            Provider.MARKETAUX to 4,
            Provider.ALPHAVANTAGE to 5,
        )

        /**
         * Provider priority for INFO operations (lower = higher priority)
         * Order: FINNHUB → MASSIVE → FMP → STOCKDATA → MARKETSTACK → ALPHAVANTAGE
         */
        val INFO_PRIORITY = mapOf(
            Provider.FINNHUB to 1,
            Provider.MASSIVE to 2,
            Provider.FINANCIALMODELINGPREP to 3,
            Provider.STOCKDATA to 4,
            Provider.MARKETSTACK to 5,
            Provider.ALPHAVANTAGE to 6,
        )
    }

    suspend fun search(query: String): List<TickerDto> {
        val cached = searchCache.get(query)

        if (cached == null) {
            val providers = searchProviders.toMutableMap()

            // Pre-fetch all provider usages once for the entire batch operation
            val preFetchedUsages = if (providers.isNotEmpty()) {
                limitUsageService.getUsagesBatch(providers.keys)
            } else {
                null
            }

            while (providers.isNotEmpty()) {
                val (provider, result) = makeCall(
                    candidates = providers,
                    preFetchedUsages = preFetchedUsages,
                    priorityMap = SEARCH_PRIORITY,
                ) {
                    it.search(query)
                }

                if (result.isNotEmpty()) {
                    searchCache.put(query, result)

                    return result
                } else {
                    providers.remove(provider)
                }
            }

            return emptyList()
        } else {
            return cached
        }
    }

    suspend fun news(tickers: Collection<String>, limit: Int): Map<String, List<TickerNewsDto>> {
        val cached = mutableMapOf<String, List<TickerNewsDto>>()
        val nonCached = mutableSetOf<String>()
        val toCache = mutableMapOf<String, List<TickerNewsDto>>()
        val providers = newsProviders.toMutableMap()
        var remainingLimit = limit

        newsCache.getCollection(tickers).forEach { (ticker, news) ->
            if (news == null) {
                nonCached.add(ticker)
            } else {
                cached[ticker] = news
            }
        }

        // Pre-fetch all provider usages once for the entire batch operation
        // This reduces Firestore reads from N*M to 1 (where N = tickers, M = providers)
        val preFetchedUsages = if (nonCached.isNotEmpty() && providers.isNotEmpty()) {
            limitUsageService.getUsagesBatch(providers.keys)
        } else {
            null
        }

        while (nonCached.isNotEmpty()) {
            val ticker = nonCached.first()

            runCatching {
                makeCall(
                    candidates = providers,
                    priorityMap = NEWS_PRIORITY,
                    preFetchedUsages = preFetchedUsages,
                    call = { provider ->
                        provider.news(ticker = ticker, limit = remainingLimit)
                    },
                )
            }.onSuccess { (provider, result) ->
                if (result.size < remainingLimit) {
                    providers.remove(provider)
                    remainingLimit -= result.size
                } else {
                    remainingLimit = limit
                    nonCached.remove(ticker)
                }

                ((cached[ticker] ?: emptyList()) + result).let { items ->
                    cached[ticker] = items
                    toCache[ticker] = items
                }
            }.onFailure { error ->
                if (error is AllProvidersHaveReachedTheirLimitsException && cached.isEmpty()) {
                    throw error
                } else if (error is NoAvailableProviderException) {
                    providers.remove(error.provider)
                }
            }
        }

        newsCache.putCollection(toCache)

        return cached
    }

    suspend fun info(tickers: Collection<String>): Map<String, TickerInfoDto> {
        val cached = mutableMapOf<String, TickerInfoDto>()
        val toCache = mutableMapOf<String, TickerInfoDto>()
        val nonCached = mutableSetOf<String>()
        val providers = infoProviders.toMutableMap()

        infoCache.getCollection(tickers).forEach { (ticker, info) ->
            if (info == null) {
                nonCached.add(ticker)
            } else {
                cached[ticker] = info
            }
        }

        // Pre-fetch all provider usages once for the entire batch operation
        val preFetchedUsages = if (nonCached.isNotEmpty() && providers.isNotEmpty()) {
            limitUsageService.getUsagesBatch(providers.keys)
        } else {
            null
        }

        while (nonCached.isNotEmpty()) {
            runCatching {
                makeCall(
                    candidates = providers,
                    priorityMap = INFO_PRIORITY,
                    preFetchedUsages = preFetchedUsages,
                    call = { provider ->
                        provider.info(nonCached)
                    }
                )
            }.onSuccess { (provider, result) ->
                result.forEach { (ticker, info) ->
                    cached[ticker] = info
                    nonCached.remove(ticker)
                    toCache[ticker] = info
                }

                if (result.isEmpty()) {
                    providers.remove(provider)
                }
            }.onFailure { error ->
                if (error is AllProvidersHaveReachedTheirLimitsException && cached.isEmpty()) {
                    throw error
                } else if (error is NoAvailableProviderException) {
                    providers.remove(error.provider)
                }
            }
        }

        infoCache.putCollection(toCache)

        return cached
    }

    private suspend fun <T, P> makeCall(
        candidates: Map<Provider, P>,
        priorityMap: Map<Provider, Int>,
        preFetchedUsages: Map<Provider, LimitUsage>? = null,
        call: suspend (P) -> T
    ): Pair<Provider, T> {
        val (provider, config) = getAvailableProvider(
            candidates.keys,
            priorityMap,
            preFetchedUsages
        )

        // Always use transactional increment for correctness across multiple instances
        // The transaction ensures atomic read-check-update even with concurrent requests
        limitUsageService.tryIncrementUsage(provider, config.limit)
            ?: throw NoAvailableProviderException(
                "Provider $provider has reached its limit",
                provider,
            )

        statsService.recordSelection(provider)

        return try {
            provider to call(requireNotNull(candidates[provider]))
        } catch (e: Exception) {
            statsService.recordFailure(provider)
            throw e
        }
    }

    /**
     * Find an available provider from the candidates based on priority ranking
     *
     * Selection algorithm:
     * 1. Filter providers that have remaining capacity
     * 2. Sort by priority (lower priority number = higher priority)
     * 3. Among providers with the same priority, prefer those with more remaining capacity
     * 4. Select the highest priority available provider
     *
     * @param preFetchedUsages Optional pre-fetched usages map to avoid redundant Firestore reads
     */
    private suspend fun getAvailableProvider(
        candidates: Set<Provider>,
        priorityMap: Map<Provider, Int>,
        preFetchedUsages: Map<Provider, LimitUsage>? = null
    ): Pair<Provider, ProviderConfig> {
        val configs = providerConfigService.configs
        val now = LocalDateTime.now()

        // Batch fetch all usages if not provided (optimization for multiple providers)
        val usages = preFetchedUsages ?: if (candidates.size > 1) {
            limitUsageService.getUsagesBatch(candidates)
        } else {
            // Single provider - use individual getUsage (still benefits from cache)
            candidates.associateWith { limitUsageService.getUsage(it) }
        }

        // Get all available providers with their priority and remaining capacity
        val availableProviders = candidates
            .mapNotNull { provider ->
                val config = configs[provider] ?: return@mapNotNull null
                val usage = usages[provider] ?: LimitUsage()

                if (usage.canUse(config.limit, now)) {
                    val remainingCapacity = usage.getRemainingCapacity(config.limit, now)
                    val priority = priorityMap[provider] ?: Int.MAX_VALUE

                    ProviderCandidate(provider, config, priority, remainingCapacity)
                } else {
                    null
                }
            }
            // Sort by priority first (ascending), then by remaining capacity (descending)
            .sortedWith(
                compareBy<ProviderCandidate> { it.priority }
                    .thenByDescending { it.remainingCapacity }
            )

        if (availableProviders.isEmpty()) {
            throw AllProvidersHaveReachedTheirLimitsException()
        }

        // Select the highest priority provider (first in sorted list)
        val selected = availableProviders.first()

        return selected.provider to selected.config
    }

    /**
     * Data class to hold provider candidate information during selection
     */
    private data class ProviderCandidate(
        val provider: Provider,
        val config: ProviderConfig,
        val priority: Int,
        val remainingCapacity: Int,
    )
}

class AllProvidersHaveReachedTheirLimitsException() :
    Exception("All providers have reached their limits")
