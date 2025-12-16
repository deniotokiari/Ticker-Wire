package pl.deniotokiari.tickerwire.services

import kotlinx.serialization.serializer
import pl.deniotokiari.tickerwire.model.AppTtlConfig
import pl.deniotokiari.tickerwire.model.TtlConfig

private const val TTL = "ttl"
private const val TTL_SERVER = "server"
private const val TTL_CLIENT = "client"

class TtlConfigService(
    private val firebaseRemoteConfigService: FirebaseRemoteConfigService,
) {
    private var _ttlConfig: AppTtlConfig = AppTtlConfig(
        server = TtlConfig(
            searchTtlMs = 360000L,
            newsTtlMs = 360000L,
            infoTtlMs = 2160000L,
        ),
        client = TtlConfig(
            searchTtlMs = 2 * 360000L,
            newsTtlMs = 2 * 360000L,
            infoTtlMs = 2 * 2160000L,
        ),
    )
    val ttlConfig: AppTtlConfig get() = _ttlConfig

    init {
        refresh()
    }

    fun refresh() {
        val result = firebaseRemoteConfigService.get(
            key = TTL,
            kSerializer = serializer<AppTtlConfig>(),
        ) ?: return

        _ttlConfig = result
    }
}
