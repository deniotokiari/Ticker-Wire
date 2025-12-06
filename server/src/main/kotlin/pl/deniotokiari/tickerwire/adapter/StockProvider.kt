package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig
import pl.deniotokiari.tickerwire.services.FirestoreLimitUsageService
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheService
import java.time.LocalDateTime
import kotlin.collections.toMutableMap

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
) : StockNewsProvider, StockSearchProvider, StockInfoProvider {

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

    override suspend fun search(query: String): List<TickerDto> {
        val cached = searchCache.get(query)

        return if (cached == null) {
            val (_, result) = makeCall(searchProviders, SEARCH_PRIORITY) {
                it.search(query)
            }

            if (result.isNotEmpty()) {
                searchCache.put(query, result)
            }

            result
        } else {
            cached
        }
    }

    override suspend fun news(tickers: Collection<String>): Map<String, List<TickerNewsDto>> {
        return batchCall(
            tickers = tickers,
            candidates = newsProviders,
            priorityMap = NEWS_PRIORITY,
            cache = newsCache,
            call = StockNewsProvider::news,
        )
    }

    override suspend fun info(tickers: Collection<String>): Map<String, TickerInfoDto> {
        return batchCall(
            tickers = tickers,
            candidates = infoProviders,
            priorityMap = INFO_PRIORITY,
            cache = infoCache,
            call = StockInfoProvider::info,
        )
    }

    private suspend fun <T, P> makeCall(
        candidates: Map<Provider, P>,
        priorityMap: Map<Provider, Int>,
        call: suspend (P) -> T
    ): Pair<Provider, T> {
        val (provider, config) = getAvailableProvider(candidates.keys, priorityMap)

        limitUsageService.tryIncrementUsage(provider, config.limit)
            ?: throw NoAvailableProviderException("Provider $provider has reached its limit")

        statsService.recordSelection(provider)

        return try {
            provider to call(requireNotNull(candidates[provider]))
        } catch (e: Exception) {
            statsService.recordFailure(provider)
            throw e
        }
    }

    private suspend fun <StockProvider, Result : Any> batchCall(
        tickers: Collection<String>,
        candidates: Map<Provider, StockProvider>,
        priorityMap: Map<Provider, Int>,
        cache: FirestoreCacheService<Result>,
        call: suspend (StockProvider, Collection<String>) -> Map<String, Result>,
    ): Map<String, Result> {
        val cached = mutableMapOf<String, Result>()
        val nonCached = mutableSetOf<String>()
        val candidates = candidates.toMutableMap()

        tickers.forEach { ticker ->
            val data = cache.get(ticker)

            if (data == null) {
                nonCached.add(ticker)
            } else {
                cached[ticker] = data
            }
        }

        while (nonCached.isNotEmpty()) {
            runCatching {
                makeCall(candidates, priorityMap) { provider ->
                    call(
                        provider,
                        nonCached,
                    )
                }
            }.onSuccess { (provider, result) ->
                for ((symbol, dto) in result) {
                    if (dto is Collection<*> && dto.isNotEmpty()) {
                        cache.put(symbol, dto)
                    } else {
                        cache.put(symbol, dto)
                    }

                    cached[symbol] = dto
                    nonCached.remove(symbol)
                }

                if (result.isEmpty()) {
                    candidates.remove(provider)
                }
            }.onFailure { error ->
                if (error is AllProvidersHaveReachedTheirLimitsException && cached.isEmpty()) {
                    throw error
                }
            }
        }

        return cached
    }

    /**
     * Find an available provider from the candidates based on priority ranking
     *
     * Selection algorithm:
     * 1. Filter providers that have remaining capacity
     * 2. Sort by priority (lower priority number = higher priority)
     * 3. Among providers with the same priority, prefer those with more remaining capacity
     * 4. Select the highest priority available provider
     */
    private suspend fun getAvailableProvider(
        candidates: Set<Provider>,
        priorityMap: Map<Provider, Int>
    ): Pair<Provider, ProviderConfig> {
        val configs = providerConfigService.configs
        val now = LocalDateTime.now()

        // Get all available providers with their priority and remaining capacity
        val availableProviders = candidates
            .mapNotNull { provider ->
                val config = configs[provider] ?: return@mapNotNull null
                val usage = limitUsageService.getUsage(provider)

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
            throw NoAvailableProviderException("All providers have reached their limits")
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

/**
 * Exception thrown when no provider is available due to rate limits
 */
class NoAvailableProviderException(message: String) : Exception(message)

class AllProvidersHaveReachedTheirLimitsException() :
    Exception("All providers have reached their limits")
