package pl.deniotokiari.tickerwire.common.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeUseCasesTest {

    // Fake repository for testing
    private class FakeAppSettingsRepository {
        private var _isDarkTheme: Boolean = false
        val isDarkTheme: Boolean get() = _isDarkTheme

        fun setDarkTheme(value: Boolean) {
            _isDarkTheme = value
        }
    }

    // Testable use cases
    private class TestableApplyDarkThemeUseCase(
        private val repository: FakeAppSettingsRepository
    ) {
        operator fun invoke() {
            repository.setDarkTheme(true)
        }
    }

    private class TestableApplyLightThemeUseCase(
        private val repository: FakeAppSettingsRepository
    ) {
        operator fun invoke() {
            repository.setDarkTheme(false)
        }
    }

    private class TestableIsDarkThemeUseCase(
        private val repository: FakeAppSettingsRepository
    ) {
        operator fun invoke(): Boolean = repository.isDarkTheme
    }

    @Test
    fun applyDarkThemeSetsDarkTheme() {
        val repository = FakeAppSettingsRepository()
        val useCase = TestableApplyDarkThemeUseCase(repository)

        useCase()

        assertTrue(repository.isDarkTheme)
    }

    @Test
    fun applyLightThemeSetsLightTheme() {
        val repository = FakeAppSettingsRepository()
        repository.setDarkTheme(true)
        val useCase = TestableApplyLightThemeUseCase(repository)

        useCase()

        assertFalse(repository.isDarkTheme)
    }

    @Test
    fun isDarkThemeReturnsTrueWhenDarkThemeEnabled() {
        val repository = FakeAppSettingsRepository()
        repository.setDarkTheme(true)
        val useCase = TestableIsDarkThemeUseCase(repository)

        val result = useCase()

        assertTrue(result)
    }

    @Test
    fun isDarkThemeReturnsFalseWhenLightThemeEnabled() {
        val repository = FakeAppSettingsRepository()
        repository.setDarkTheme(false)
        val useCase = TestableIsDarkThemeUseCase(repository)

        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun themeTogglesBetweenDarkAndLight() {
        val repository = FakeAppSettingsRepository()
        val applyDark = TestableApplyDarkThemeUseCase(repository)
        val applyLight = TestableApplyLightThemeUseCase(repository)
        val isDark = TestableIsDarkThemeUseCase(repository)

        assertFalse(isDark()) // Initially light

        applyDark()
        assertTrue(isDark())

        applyLight()
        assertFalse(isDark())

        applyDark()
        assertTrue(isDark())
    }

    @Test
    fun multipleApplyDarkThemeCallsRemainDark() {
        val repository = FakeAppSettingsRepository()
        val applyDark = TestableApplyDarkThemeUseCase(repository)
        val isDark = TestableIsDarkThemeUseCase(repository)

        applyDark()
        applyDark()
        applyDark()

        assertTrue(isDark())
    }

    @Test
    fun multipleApplyLightThemeCallsRemainLight() {
        val repository = FakeAppSettingsRepository()
        val applyLight = TestableApplyLightThemeUseCase(repository)
        val isDark = TestableIsDarkThemeUseCase(repository)

        applyLight()
        applyLight()
        applyLight()

        assertFalse(isDark())
    }
}

