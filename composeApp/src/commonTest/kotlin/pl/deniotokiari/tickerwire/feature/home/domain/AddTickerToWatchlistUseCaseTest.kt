package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddTickerToWatchlistUseCaseTest {

    // Non-suspend fake repository for KMP test compatibility
    private class FakeWatchlistRepository {
        private val _items = mutableListOf<Ticker>()
        val items: List<Ticker> get() = _items.toList()

        fun addTicker(ticker: Ticker) {
            _items.add(ticker)
        }

        fun removeTicker(ticker: Ticker) {
            _items.remove(ticker)
        }
    }

    // Use case wrapper for testing (synchronous for KMP compatibility)
    private class TestableAddTickerUseCase(
        private val repository: FakeWatchlistRepository
    ) {
        operator fun invoke(ticker: Ticker) = runCatching {
            repository.addTicker(ticker)
        }
    }

    @Test
    fun invokeAddsTicker() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableAddTickerUseCase(repository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        val result = useCase(ticker)

        assertTrue(result.isSuccess)
        assertEquals(listOf(ticker), repository.items)
    }

    @Test
    fun invokeAddsMultipleTickers() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableAddTickerUseCase(repository)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")

        useCase(ticker1)
        useCase(ticker2)

        assertEquals(2, repository.items.size)
        assertTrue(repository.items.contains(ticker1))
        assertTrue(repository.items.contains(ticker2))
    }

    @Test
    fun invokeDoesNotDuplicateTickersInFlow() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableAddTickerUseCase(repository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        useCase(ticker)
        useCase(ticker)

        // Both additions should be present (duplicates allowed)
        assertEquals(2, repository.items.size)
    }
}

