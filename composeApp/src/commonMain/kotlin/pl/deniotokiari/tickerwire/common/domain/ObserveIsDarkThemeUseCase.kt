package pl.deniotokiari.tickerwire.common.domain

import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.AppSettingsRepository

@Factory
class ObserveIsDarkThemeUseCase(
    private val appSettingsRepository: AppSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = appSettingsRepository.isDarkTheme
}
