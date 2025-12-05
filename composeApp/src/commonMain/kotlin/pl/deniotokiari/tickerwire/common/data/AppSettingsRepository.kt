package pl.deniotokiari.tickerwire.common.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.store.AppSettingsLocalDataStore

@Single
class AppSettingsRepository(
    private val appSettingsLocalDataStore: AppSettingsLocalDataStore,
) {
    private val _isDarkTheme = MutableStateFlow(appSettingsLocalDataStore.isDarkTheme())
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    suspend fun applyLightTheme() {
        _isDarkTheme.emit(false)

        appSettingsLocalDataStore.setDarkTheme(false)
    }

    suspend fun applyDarkTheme() {
        _isDarkTheme.emit(true)

        appSettingsLocalDataStore.setDarkTheme(true)
    }
}
