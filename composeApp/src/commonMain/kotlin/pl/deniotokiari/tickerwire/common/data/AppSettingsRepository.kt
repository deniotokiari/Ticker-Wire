package pl.deniotokiari.tickerwire.common.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.serializer
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.cache.MemoryCache
import pl.deniotokiari.tickerwire.common.data.cache.PersistentCache
import pl.deniotokiari.tickerwire.common.data.cache.TwoLayerCache
import pl.deniotokiari.tickerwire.common.data.store.AppSettingsLocalDataStore
import pl.deniotokiari.tickerwire.common.data.store.AppSettingsRemoteDataStore
import pl.deniotokiari.tickerwire.common.data.store.KeyValueLocalDataSource
import pl.deniotokiari.tickerwire.common.etc.Logger
import pl.deniotokiari.tickerwire.model.TtlConfig
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val NAME_TTL = "ttl"
private const val TTL_CLIENT = "ttl_client"
private const val LIMIT_TTL_CACHE = 1
private const val TTL_MEMORY = 1
private const val TTL_PERSISTENT = 1
private const val DEFAULT_SEARCH_TTL_MS = 720000L
private const val DEFAULT_NEWS_TTL_MS = 720000L
private const val DEFAULT_INFO_TTL_MS = 4320000L

@Single
class AppSettingsRepository(
    private val connectivityRepository: ConnectivityRepository,
    private val appSettingsLocalDataStore: AppSettingsLocalDataStore,
    private val appSettingsRemoteDataStore: AppSettingsRemoteDataStore,
    @Named(NAME_TTL) private val ttlCache: TwoLayerCache<TtlConfig>,
) {
    private val _isDarkTheme = MutableStateFlow(appSettingsLocalDataStore.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Initialize with default values immediately so cache providers can access it during Koin initialization
    private var _ttlConfig: TtlConfig = TtlConfig(
        searchTtlMs = 7200000L,  // 2 hours (default, will be refreshed from server)
        newsTtlMs = 7200000L,    // 2 hours (default, will be refreshed from server)
        infoTtlMs = 43200000L,   // 12 hours (default, will be refreshed from server)
    )
    val ttlConfig: TtlConfig get() = _ttlConfig

    fun applyDefaultTtlConfig() {
        _ttlConfig = TtlConfig(
            searchTtlMs = DEFAULT_SEARCH_TTL_MS,
            newsTtlMs = DEFAULT_NEWS_TTL_MS,
            infoTtlMs = DEFAULT_INFO_TTL_MS,
        )
    }

    suspend fun refreshTtlConfig() {
        _ttlConfig = getCacheTtl()
    }

    suspend fun applyLightTheme() {
        _isDarkTheme.emit(false)

        appSettingsLocalDataStore.setDarkTheme(false)
    }

    suspend fun applyDarkTheme() {
        _isDarkTheme.emit(true)

        appSettingsLocalDataStore.setDarkTheme(true)
    }

    private suspend fun getCacheTtl(): TtlConfig {
        return ttlCache.getOrFetch(
            key = TTL_CLIENT,
            ttlSkip = !connectivityRepository.isOnline(),
        ) {
            appSettingsRemoteDataStore.ttl()
        }
    }
}

@Named(NAME_TTL)
@Single
fun provideTtlCache(logger: Logger) = TwoLayerCache<TtlConfig>(
    memoryCache = MemoryCache(
        limit = LIMIT_TTL_CACHE,
        ttl = TTL_MEMORY.toDuration(DurationUnit.HOURS).inWholeMilliseconds,
    ),
    persistentCache = PersistentCache(
        ttl = TTL_PERSISTENT.toDuration(DurationUnit.DAYS).inWholeMilliseconds,
        keyValueLocalDataSource = KeyValueLocalDataSource(NAME_TTL),
        kSerializer = serializer(),
    ),
    logger = logger,
)
