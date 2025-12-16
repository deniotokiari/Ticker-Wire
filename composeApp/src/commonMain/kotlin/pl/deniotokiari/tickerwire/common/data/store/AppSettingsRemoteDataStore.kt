package pl.deniotokiari.tickerwire.common.data.store

import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.config.API_BASE_URL
import pl.deniotokiari.tickerwire.common.data.HttpClient
import pl.deniotokiari.tickerwire.model.TtlConfig

private const val API_URI = "$API_BASE_URL/api/v1"

@Single
class AppSettingsRemoteDataStore(
    private val httpClient: HttpClient,
) {
    suspend fun ttl(): TtlConfig {
        return httpClient.get("$API_URI/ttl/client")
    }
}
