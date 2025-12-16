package pl.deniotokiari.tickerwire.feature.home.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class ClearTickersDataUseCaseTest {

    private class FakeTickerRepository {
        var clearCalled = false

        fun clear() {
            clearCalled = true
        }
    }

    private class TestableClearTickersDataUseCase(
        private val repository: FakeTickerRepository
    ) {
        operator fun invoke() {
            repository.clear()
        }
    }

    @Test
    fun invokeCallsRepositoryClear() {
        val repository = FakeTickerRepository()
        val useCase = TestableClearTickersDataUseCase(repository)

        useCase()

        assertTrue(repository.clearCalled)
    }

    @Test
    fun invokeCanBeCalledMultipleTimes() {
        val repository = FakeTickerRepository()
        val useCase = TestableClearTickersDataUseCase(repository)

        useCase()
        useCase()
        useCase()

        assertTrue(repository.clearCalled)
    }
}

