package pl.deniotokiari.tickerwire.common.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.AppSettingsRepository

@Factory
class ApplyLightThemeUseCase(
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke() = runCatching {
        appSettingsRepository.applyLightTheme()
    }
}
