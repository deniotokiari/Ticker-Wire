package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveWatchlistItemsUseCaseTest {

    private class FakeWatchlistRepository {
        private val _items = mutableListOf<Ticker>()
        val items: List<Ticker> get() = _items.toList()

        fun addTicker(ticker: Ticker) {
            _items.add(ticker)
        }

        fun removeTicker(ticker: Ticker) {
            _items.remove(ticker)
        }

        fun setItems(newItems: List<Ticker>) {
            _items.clear()
            _items.addAll(newItems)
        }
    }

    private class TestableObserveWatchlistItemsUseCase(
        private val repository: FakeWatchlistRepository
    ) {
        operator fun invoke(): List<Ticker> = repository.items
    }

    @Test
    fun invokeReturnsEmptyListInitially() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableObserveWatchlistItemsUseCase(repository)

        val result = useCase()

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsTickersAfterAdding() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableObserveWatchlistItemsUseCase(repository)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")

        repository.addTicker(ticker1)
        repository.addTicker(ticker2)

        val result = useCase()

        assertEquals(2, result.size)
        assertTrue(result.contains(ticker1))
        assertTrue(result.contains(ticker2))
    }

    @Test
    fun invokeReturnsUpdatedListAfterRemoval() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableObserveWatchlistItemsUseCase(repository)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")

        repository.addTicker(ticker1)
        repository.addTicker(ticker2)
        repository.removeTicker(ticker1)

        val result = useCase()

        assertEquals(1, result.size)
        assertTrue(result.contains(ticker2))
        assertTrue(!result.contains(ticker1))
    }

    @Test
    fun invokeReturnsMultipleTickers() {
        val repository = FakeWatchlistRepository()
        val useCase = TestableObserveWatchlistItemsUseCase(repository)
        val tickers = listOf(
            Ticker(symbol = "AAPL", company = "Apple Inc"),
            Ticker(symbol = "GOOGL", company = "Alphabet Inc"),
            Ticker(symbol = "MSFT", company = "Microsoft"),
            Ticker(symbol = "TSLA", company = "Tesla")
        )

        repository.setItems(tickers)

        val result = useCase()

        assertEquals(4, result.size)
        assertEquals(tickers, result)
    }
}

