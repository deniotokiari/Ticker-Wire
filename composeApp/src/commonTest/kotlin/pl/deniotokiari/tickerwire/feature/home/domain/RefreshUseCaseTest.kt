package pl.deniotokiari.tickerwire.feature.home.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class RefreshUseCaseTest {

    private class FakeTickerRepository {
        var refreshCalled = false

        fun refresh() {
            refreshCalled = true
        }
    }

    private class TestableRefreshUseCase(
        private val tickerRepository: FakeTickerRepository
    ) {
        operator fun invoke() {
            tickerRepository.refresh()
        }
    }

    @Test
    fun invokeCallsRepositoryRefresh() {
        val repository = FakeTickerRepository()
        val useCase = TestableRefreshUseCase(repository)

        useCase()

        assertTrue(repository.refreshCalled)
    }

    @Test
    fun invokeCanBeCalledMultipleTimes() {
        val repository = FakeTickerRepository()
        val useCase = TestableRefreshUseCase(repository)

        useCase()
        assertTrue(repository.refreshCalled)

        repository.refreshCalled = false
        useCase()
        assertTrue(repository.refreshCalled)
    }
}

