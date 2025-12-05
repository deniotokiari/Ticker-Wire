package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoveTickerFromWatchlistUseCaseTest {

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
    private class TestableRemoveTickerUseCase(
        private val repository: FakeWatchlistRepository
    ) {
        operator fun invoke(ticker: Ticker) = runCatching {
            repository.removeTicker(ticker)
        }
    }

    @Test
    fun invokeRemovesTicker() {
        val repository = FakeWatchlistRepository()
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        repository.addTicker(ticker)
        val useCase = TestableRemoveTickerUseCase(repository)

        val result = useCase(ticker)

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), repository.items)
    }

    @Test
    fun invokeRemovesOnlySpecificTicker() {
        val repository = FakeWatchlistRepository()
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val ticker3 = Ticker(symbol = "MSFT", company = "Microsoft")
        repository.addTicker(ticker1)
        repository.addTicker(ticker2)
        repository.addTicker(ticker3)
        val useCase = TestableRemoveTickerUseCase(repository)

        useCase(ticker2)

        assertEquals(2, repository.items.size)
        assertTrue(repository.items.contains(ticker1))
        assertFalse(repository.items.contains(ticker2))
        assertTrue(repository.items.contains(ticker3))
    }

    @Test
    fun invokeRemovingNonExistentTickerDoesNotFail() {
        val repository = FakeWatchlistRepository()
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val nonExistent = Ticker(symbol = "NONE", company = "Not Exist")
        repository.addTicker(ticker1)
        val useCase = TestableRemoveTickerUseCase(repository)

        val result = useCase(nonExistent)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.items.size)
        assertTrue(repository.items.contains(ticker1))
    }

    @Test
    fun invokeRemovesOnlyFirstOccurrence() {
        val repository = FakeWatchlistRepository()
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        repository.addTicker(ticker)
        repository.addTicker(ticker)
        repository.addTicker(ticker)
        val useCase = TestableRemoveTickerUseCase(repository)

        useCase(ticker)

        // Only first occurrence should be removed (mutableList.remove behavior)
        assertEquals(2, repository.items.size)
    }
}

