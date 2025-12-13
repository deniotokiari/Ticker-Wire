package pl.deniotokiari.tickerwire.common.data

import kotlinx.serialization.serializer
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.cache.MemoryCache
import pl.deniotokiari.tickerwire.common.data.cache.PersistentCache
import pl.deniotokiari.tickerwire.common.data.cache.TwoLayerCache
import pl.deniotokiari.tickerwire.common.data.store.KeyValueLocalDataSource
import pl.deniotokiari.tickerwire.common.data.store.TickerRemoteDataSource
import pl.deniotokiari.tickerwire.common.etc.Logger
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val NAME_SEARCH = "search"
private const val NAME_NEWS = "news"
private const val NAME_INFO = "info"

@Single
class TickerRepository(
    private val tickerRemoteDataSource: TickerRemoteDataSource,
    private val connectivityRepository: ConnectivityRepository,
    private val logger: Logger,
) {
    private val searchCache = TwoLayerCache<List<Ticker>>(
        memoryCache = MemoryCache(
            limit = 10,
            ttl = 30.toDuration(DurationUnit.MINUTES).inWholeMilliseconds,
        ),
        persistentCache = PersistentCache(
            ttl = 7.toDuration(DurationUnit.DAYS).inWholeMilliseconds,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_SEARCH),
            kSerializer = serializer(),
        ),
        logger = logger,
    )
    private val newsCache = TwoLayerCache<List<TickerNews>>(
        memoryCache = MemoryCache(
            limit = 50,
            ttl = 5.toDuration(DurationUnit.MINUTES).inWholeMilliseconds,
        ),
        persistentCache = PersistentCache(
            ttl = 1.toDuration(DurationUnit.HOURS).inWholeMilliseconds,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_NEWS),
            kSerializer = serializer(),
        ),
        logger = logger,
    )
    private val infoCache = TwoLayerCache<TickerData>(
        memoryCache = MemoryCache(
            limit = 10,
            ttl = 15.toDuration(DurationUnit.MINUTES).inWholeMilliseconds,
        ),
        persistentCache = PersistentCache(
            ttl = 1.toDuration(DurationUnit.DAYS).inWholeMilliseconds,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_INFO),
            kSerializer = serializer(),
        ),
        logger = logger,
    )

    private val isOnline: Boolean get() = connectivityRepository.isOnline()

    suspend fun search(query: String): List<Ticker> {
        val key = query.trim().lowercase()

        return searchCache.getOrFetch(key = key, ttlSkip = !isOnline) {
            tickerRemoteDataSource
                .search(query)
                .distinctBy { item -> "${item.ticker}${item.company}" }
                .map { dto -> Ticker(symbol = dto.ticker, company = dto.company) }
        }
    }

    fun cachedNews(tickers: List<Ticker>, ttlSkip: Boolean): List<TickerNews> {
        return tickers
            .mapNotNull { ticker -> newsCache.get(key = ticker.symbol, ttlSkip = ttlSkip) }
            .flatten()
            .distinctBy { item -> item.title }
            .sortedByDescending { item -> item.timestamp }
    }

    suspend fun news(tickers: List<Ticker>): List<TickerNews> {
        val cached = mutableListOf<TickerNews>()
        val nonCached = mutableMapOf<String, Ticker>()

        tickers.forEach { ticker ->
            val news = newsCache.get(key = ticker.symbol, ttlSkip = !isOnline)

            if (news == null) {
                nonCached[ticker.symbol] = ticker
            } else {
                cached.addAll(news)
            }
        }

        if (nonCached.isNotEmpty() && isOnline) {
            tickerRemoteDataSource
                .news(nonCached.values.toList())
                .forEach { (symbol, dto) ->
                    val ticker = nonCached[symbol]

                    if (ticker != null) {
                        val news = dto
                            .map { item ->
                                TickerNews(
                                    ticker = ticker,
                                    title = item.title.trim(),
                                    provider = item.provider,
                                    dateTimeFormatted = item.dateTimeFormatted.takeWhile { char -> !char.isWhitespace() },
                                    timestamp = item.timestamp,
                                    url = item.url,
                                )
                            }
                            .distinctBy { item -> item.title }
                            .sortedByDescending { item -> item.timestamp }
                            .take(5)

                        newsCache.put(key = symbol, data = news)

                        cached.addAll(news)
                    }
                }
        }

        return cached
            .distinctBy { item -> item.title }
            .sortedByDescending { item -> item.timestamp }
    }

    fun cachedInfo(tickers: List<Ticker>, ttlSkip: Boolean): Map<Ticker, TickerData> {
        return tickers
            .mapNotNull { ticker -> infoCache.get(key = ticker.symbol, ttlSkip = ttlSkip) }
            .associateBy { item -> item.ticker }
    }

    suspend fun info(tickers: List<Ticker>): Map<Ticker, TickerData> {
        val cached = mutableMapOf<Ticker, TickerData>()
        val nonCached = mutableMapOf<String, Ticker>()

        tickers.forEach { ticker ->
            val info = infoCache.get(key = ticker.symbol, ttlSkip = !isOnline)

            if (info == null) {
                nonCached[ticker.symbol] = ticker
            } else {
                cached[ticker] = info
            }
        }

        if (nonCached.isNotEmpty() && isOnline) {
            tickerRemoteDataSource
                .info(nonCached.values.toList())
                .forEach { (symbol, dto) ->
                    val ticker = nonCached[symbol]

                    if (ticker != null) {
                        val info = TickerData(
                            ticker = ticker,
                            marketValueFormatted = dto.marketValueFormatted,
                            deltaFormatted = dto.deltaFormatted,
                            percentFormatted = dto.percentFormatted,
                            currency = dto.currency,
                        )

                        infoCache.put(key = symbol, data = info)

                        cached[ticker] = info
                    }
                }
        }

        return cached
    }

    fun clear() {
        if (!isOnline) return

        searchCache.clear()
        infoCache.clear()
        newsCache.clear()
    }
}
