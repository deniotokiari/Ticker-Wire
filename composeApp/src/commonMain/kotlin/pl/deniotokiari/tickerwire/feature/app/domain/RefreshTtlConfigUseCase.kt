package pl.deniotokiari.tickerwire.feature.app.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.AppSettingsRepository

@Factory
class RefreshTtlConfigUseCase(
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke() = runCatching {
        appSettingsRepository.refreshTtlConfig()
    }.onFailure { appSettingsRepository.applyDefaultTtlConfig() }
}
