package pl.deniotokiari.tickerwire.services

import kotlinx.serialization.serializer
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig

class ProviderConfigService(
    private val firebaseRemoteConfigService: FirebaseRemoteConfigService,
) {
    private val _configs = mutableMapOf<Provider, ProviderConfig>()
    val configs: Map<Provider, ProviderConfig>
        get() = _configs

    init {
        refresh()
    }

    fun refresh() {
        val result = firebaseRemoteConfigService
            .get(
                key = "providers",
                kSerializer = serializer<Map<String, ProviderConfig>>(),
            )
            ?: return

        _configs.clear()

        result.forEach { (key, value) ->
            _configs[Provider.valueOf(key)] = value
        }
    }

    fun get(provider: Provider): ProviderConfig {
        return requireNotNull(_configs[provider])
    }

    fun set(provider: Provider, config: ProviderConfig) {
        _configs[provider] = config

        firebaseRemoteConfigService.set(
            key = "providers",
            value = _configs.mapKeys { (key, _) -> key.name },
            kSerializer = serializer<Map<String, ProviderConfig>>(),
        )
    }

    fun setAsync(provider: Provider, config: ProviderConfig) {
        _configs[provider] = config

        firebaseRemoteConfigService.setAsync(
            key = "providers",
            value = _configs.mapKeys { (key, _) -> key.name },
            kSerializer = serializer<Map<String, ProviderConfig>>(),
        )
    }
}
