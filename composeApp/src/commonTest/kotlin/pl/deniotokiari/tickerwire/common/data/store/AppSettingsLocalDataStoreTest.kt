package pl.deniotokiari.tickerwire.common.data.store

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsLocalDataStoreTest {

    private class FakeKeyValueLocalDataSource {
        private val storage = mutableMapOf<String, Boolean>()

        fun getBoolean(key: String): Boolean? = storage[key]

        fun setBoolean(key: String, value: Boolean) {
            storage[key] = value
        }
    }

    private class TestableAppSettingsLocalDataStore(
        private val dataStore: FakeKeyValueLocalDataSource
    ) {
        private val KEY_IS_DARK_THEME = "is_dark_theme"

        fun isDarkTheme(): Boolean = dataStore.getBoolean(KEY_IS_DARK_THEME) ?: false

        fun setDarkTheme(value: Boolean) {
            dataStore.setBoolean(KEY_IS_DARK_THEME, value)
        }
    }

    private lateinit var fakeDataStore: FakeKeyValueLocalDataSource
    private lateinit var dataStore: TestableAppSettingsLocalDataStore

    @BeforeTest
    fun setup() {
        fakeDataStore = FakeKeyValueLocalDataSource()
        dataStore = TestableAppSettingsLocalDataStore(fakeDataStore)
    }

    @Test
    fun initialIsDarkThemeReturnsFalse() {
        val result = dataStore.isDarkTheme()

        assertFalse(result)
    }

    @Test
    fun setDarkThemeTrueSetsDarkTheme() {
        dataStore.setDarkTheme(true)

        assertTrue(dataStore.isDarkTheme())
    }

    @Test
    fun setDarkThemeFalseSetsLightTheme() {
        dataStore.setDarkTheme(true)
        dataStore.setDarkTheme(false)

        assertFalse(dataStore.isDarkTheme())
    }

    @Test
    fun setDarkThemePersistsValue() {
        dataStore.setDarkTheme(true)

        val value1 = dataStore.isDarkTheme()
        val value2 = dataStore.isDarkTheme()

        assertTrue(value1)
        assertTrue(value2)
        assertEquals(value1, value2)
    }

    @Test
    fun toggleDarkTheme() {
        assertFalse(dataStore.isDarkTheme())

        dataStore.setDarkTheme(true)
        assertTrue(dataStore.isDarkTheme())

        dataStore.setDarkTheme(false)
        assertFalse(dataStore.isDarkTheme())

        dataStore.setDarkTheme(true)
        assertTrue(dataStore.isDarkTheme())
    }

    @Test
    fun multipleSetDarkThemeTrueCallsRemainTrue() {
        dataStore.setDarkTheme(true)
        dataStore.setDarkTheme(true)
        dataStore.setDarkTheme(true)

        assertTrue(dataStore.isDarkTheme())
    }

    @Test
    fun multipleSetDarkThemeFalseCallsRemainFalse() {
        dataStore.setDarkTheme(false)
        dataStore.setDarkTheme(false)
        dataStore.setDarkTheme(false)

        assertFalse(dataStore.isDarkTheme())
    }
}

