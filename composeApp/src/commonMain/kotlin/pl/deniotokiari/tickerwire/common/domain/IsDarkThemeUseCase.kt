package pl.deniotokiari.tickerwire.common.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.AppSettingsRepository

@Factory
class IsDarkThemeUseCase(
    private val appSettingsRepository: AppSettingsRepository,
) {
    operator fun invoke(): Boolean = appSettingsRepository.isDarkTheme.value
}
