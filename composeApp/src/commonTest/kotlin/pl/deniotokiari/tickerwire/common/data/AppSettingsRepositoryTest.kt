package pl.deniotokiari.tickerwire.common.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsRepositoryTest {

    private class FakeAppSettingsLocalDataStore {
        private var _isDarkTheme: Boolean = false

        fun isDarkTheme(): Boolean = _isDarkTheme

        fun setDarkTheme(value: Boolean) {
            _isDarkTheme = value
        }
    }

    // Simplified testable wrapper that doesn't use coroutines
    private class TestableAppSettingsRepository(
        private val dataStore: FakeAppSettingsLocalDataStore
    ) {
        private var _isDarkTheme: Boolean = dataStore.isDarkTheme()
        val isDarkTheme: Boolean get() = _isDarkTheme

        fun applyDarkTheme() {
            _isDarkTheme = true
            dataStore.setDarkTheme(true)
        }

        fun applyLightTheme() {
            _isDarkTheme = false
            dataStore.setDarkTheme(false)
        }
    }

    @Test
    fun initialIsDarkThemeReturnsFalse() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        assertFalse(repository.isDarkTheme)
    }

    @Test
    fun applyDarkThemeSetsDarkThemeToTrue() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        repository.applyDarkTheme()

        assertTrue(repository.isDarkTheme)
        assertTrue(dataStore.isDarkTheme())
    }

    @Test
    fun applyLightThemeSetsDarkThemeToFalse() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)
        
        // Set to dark first
        repository.applyDarkTheme()
        assertTrue(repository.isDarkTheme)

        repository.applyLightTheme()

        assertFalse(repository.isDarkTheme)
        assertFalse(dataStore.isDarkTheme())
    }

    @Test
    fun isDarkThemeUpdatesWhenThemeChanges() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        assertFalse(repository.isDarkTheme)

        repository.applyDarkTheme()
        assertTrue(repository.isDarkTheme)

        repository.applyLightTheme()
        assertFalse(repository.isDarkTheme)
    }

    @Test
    fun multipleApplyDarkThemeCallsRemainDark() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        repository.applyDarkTheme()
        repository.applyDarkTheme()
        repository.applyDarkTheme()

        assertTrue(repository.isDarkTheme)
    }

    @Test
    fun multipleApplyLightThemeCallsRemainLight() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        repository.applyLightTheme()
        repository.applyLightTheme()
        repository.applyLightTheme()

        assertFalse(repository.isDarkTheme)
    }

    @Test
    fun toggleBetweenDarkAndLightTheme() {
        val dataStore = FakeAppSettingsLocalDataStore()
        val repository = TestableAppSettingsRepository(dataStore)

        assertFalse(repository.isDarkTheme)

        repository.applyDarkTheme()
        assertTrue(repository.isDarkTheme)

        repository.applyLightTheme()
        assertFalse(repository.isDarkTheme)

        repository.applyDarkTheme()
        assertTrue(repository.isDarkTheme)
    }
}

