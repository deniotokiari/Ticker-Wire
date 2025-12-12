package pl.deniotokiari.tickerwire.common.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveIsDarkThemeUseCaseTest {

    private class FakeAppSettingsRepository {
        private var _isDarkTheme: Boolean = false
        val isDarkTheme: Boolean get() = _isDarkTheme

        fun setDarkTheme(value: Boolean) {
            _isDarkTheme = value
        }
    }

    private class TestableObserveIsDarkThemeUseCase(
        private val repository: FakeAppSettingsRepository
    ) {
        operator fun invoke(): Boolean = repository.isDarkTheme
    }

    @Test
    fun invokeReturnsFalseInitially() {
        val repository = FakeAppSettingsRepository()
        val useCase = TestableObserveIsDarkThemeUseCase(repository)

        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun invokeReturnsTrueWhenDarkThemeIsSet() {
        val repository = FakeAppSettingsRepository()
        val useCase = TestableObserveIsDarkThemeUseCase(repository)

        repository.setDarkTheme(true)
        val result = useCase()

        assertTrue(result)
    }

    @Test
    fun invokeReturnsFalseWhenLightThemeIsSet() {
        val repository = FakeAppSettingsRepository()
        val useCase = TestableObserveIsDarkThemeUseCase(repository)

        repository.setDarkTheme(true)
        assertTrue(useCase())

        repository.setDarkTheme(false)
        val result = useCase()

        assertFalse(result)
    }

    @Test
    fun invokeReturnsUpdatesWhenThemeChanges() {
        val repository = FakeAppSettingsRepository()
        val useCase = TestableObserveIsDarkThemeUseCase(repository)

        assertFalse(useCase())

        repository.setDarkTheme(true)
        assertTrue(useCase())

        repository.setDarkTheme(false)
        assertFalse(useCase())
    }
}

