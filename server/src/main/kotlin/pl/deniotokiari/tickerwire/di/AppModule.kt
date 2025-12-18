package pl.deniotokiari.tickerwire.di

import com.google.firebase.cloud.FirestoreClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pl.deniotokiari.tickerwire.services.FirebaseRemoteConfigService
import pl.deniotokiari.tickerwire.services.FirestoreLimitUsageService
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import pl.deniotokiari.tickerwire.services.RequestLimitsService
import pl.deniotokiari.tickerwire.services.TtlConfigService
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService
import pl.deniotokiari.tickerwire.services.cache.CacheCleanupScheduler
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheFactory

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 10_000L
private const val SOCKET_TIMEOUT_MS = 30_000L

val appModule = module {
    // HTTP Client with timeouts and logging
    single {
        HttpClient(CIO) {
            // Timeouts for external API calls
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }

            // Logging for debugging and monitoring
            install(Logging) {
                level = LogLevel.INFO
            }

            // JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    allowStructuredMapKeys = true
                })
            }
        }
    }

    // Firebase Services
    single { FirestoreClient.getFirestore() }
    singleOf(::FirebaseRemoteConfigService)
    single { FirestoreLimitUsageService(firestore = get()) }
    singleOf(::ProviderConfigService)
    singleOf(::TtlConfigService)
    singleOf(::RequestLimitsService)

    // Stats Service
    single { ProviderStatsService(firestore = get()) }

    // Cache Services
    single { FirestoreCacheFactory(firestore = get()) }
    single { CacheCleanupScheduler() }
}
