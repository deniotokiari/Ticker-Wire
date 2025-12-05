package pl.deniotokiari.tickerwire.common.data.store

import org.koin.core.annotation.Single

private const val NAME_APP_SETTINGS = "app_settings"
private const val KEY_IS_DARK_THEME = "is_dark_theme"

@Single
class AppSettingsLocalDataStore(
    private val dataStore: KeyValueLocalDataSource = KeyValueLocalDataSource(name = NAME_APP_SETTINGS)
) {
    fun isDarkTheme(): Boolean = dataStore.getBoolean(KEY_IS_DARK_THEME) ?: false

    fun setDarkTheme(value: Boolean) {
        dataStore.setBoolean(KEY_IS_DARK_THEME, value)
    }
}
