package pl.deniotokiari.tickerwire.common.data

import kotlinx.serialization.serializer
import org.koin.core.annotation.Named
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
import kotlin.math.roundToLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val NAME_SEARCH = "search"
private const val NAME_NEWS = "news"
private const val NAME_INFO = "info"

@Single
class TickerRepository(
    private val tickerRemoteDataSource: TickerRemoteDataSource,
    private val connectivityRepository: ConnectivityRepository,
    @Named(NAME_SEARCH) private val searchCache: Lazy<TwoLayerCache<List<Ticker>>>,
    @Named(NAME_NEWS) private val newsCache: Lazy<TwoLayerCache<List<TickerNews>>>,
    @Named(NAME_INFO) private val infoCache: Lazy<TwoLayerCache<TickerData>>,
) {
    private val isOnline: Boolean get() = connectivityRepository.isOnline()

    private val _searchCache by searchCache
    private val _newsCache by newsCache
    private val _infoCache by infoCache

    suspend fun search(query: String): List<Ticker> {
        val key = query.trim().lowercase()

        return _searchCache.getOrFetch(key = key, ttlSkip = !isOnline) {
            tickerRemoteDataSource
                .search(query)
                .distinctBy { item -> "${item.ticker}${item.company}" }
                .map { dto -> Ticker(symbol = dto.ticker, company = dto.company) }
        }
    }

    fun cachedNews(tickers: List<Ticker>, ttlSkip: Boolean): List<TickerNews> {
        return tickers
            .mapNotNull { ticker -> _newsCache.get(key = ticker.symbol, ttlSkip = ttlSkip) }
            .flatten()
            .distinctBy { item -> item.title }
            .sortedByDescending { item -> item.timestamp }
    }

    suspend fun news(tickers: List<Ticker>): List<TickerNews> {
        val cached = mutableListOf<TickerNews>()
        val nonCached = mutableMapOf<String, Ticker>()

        tickers.forEach { ticker ->
            val news = _newsCache.get(key = ticker.symbol, ttlSkip = !isOnline)

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

                        _newsCache.put(key = symbol, data = news)

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
            .mapNotNull { ticker -> _infoCache.get(key = ticker.symbol, ttlSkip = ttlSkip) }
            .associateBy { item -> item.ticker }
    }

    suspend fun info(tickers: List<Ticker>): Map<Ticker, TickerData> {
        val cached = mutableMapOf<Ticker, TickerData>()
        val nonCached = mutableMapOf<String, Ticker>()

        tickers.forEach { ticker ->
            val info = _infoCache.get(key = ticker.symbol, ttlSkip = !isOnline)

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

                        _infoCache.put(key = symbol, data = info)

                        cached[ticker] = info
                    }
                }
        }

        return cached
    }

    fun clear() {
        if (!isOnline) return

        _searchCache.clear()
        _infoCache.clear()
        _newsCache.clear()
    }
}

private const val LIMIT_SEARCH = 10
private const val LIMIT_NEWS = 100
private const val LIMIT_INFO = 10
private const val TTL_MEMORY = 0.4

@Named(NAME_SEARCH)
@Single
fun providerSearchCache(
    logger: Logger,
    appSettingsRepository: AppSettingsRepository,
): TwoLayerCache<List<Ticker>> {
    val ttl = appSettingsRepository.ttlConfig.searchTtlMs

    return TwoLayerCache(
        memoryCache = MemoryCache(
            limit = LIMIT_SEARCH,
            ttl = (ttl * TTL_MEMORY).roundToLong(),
        ),
        persistentCache = PersistentCache(
            ttl = ttl,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_SEARCH),
            kSerializer = serializer(),
        ),
        logger = logger,
    )
}

@Named(NAME_NEWS)
@Single
fun provideNewsCache(
    logger: Logger,
    appSettingsRepository: AppSettingsRepository,
): TwoLayerCache<List<TickerNews>> {
    val ttl = appSettingsRepository.ttlConfig.newsTtlMs

    return TwoLayerCache(
        memoryCache = MemoryCache(
            limit = LIMIT_NEWS,
            ttl = (ttl * TTL_MEMORY).roundToLong(),
        ),
        persistentCache = PersistentCache(
            ttl = ttl,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_NEWS),
            kSerializer = serializer(),
        ),
        logger = logger,
    )
}

@Named(NAME_INFO)
@Single
fun provideInfoCache(
    logger: Logger,
    appSettingsRepository: AppSettingsRepository,
): TwoLayerCache<TickerData> {
    val ttl = appSettingsRepository.ttlConfig.infoTtlMs

    return TwoLayerCache(
        memoryCache = MemoryCache(
            limit = LIMIT_INFO,
            ttl = (ttl * TTL_MEMORY).roundToLong(),
        ),
        persistentCache = PersistentCache(
            ttl = ttl,
            keyValueLocalDataSource = KeyValueLocalDataSource(NAME_INFO),
            kSerializer = serializer(),
        ),
        logger = logger,
    )
}
