package pl.deniotokiari.tickerwire.feature.home.data

import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchlistRepositoryTest {

    private class FakeWatchlistLocalDataSource {
        private val _items = mutableListOf<Ticker>()
        val items: List<Ticker> get() = _items.toList()

        fun getTickers(): List<Ticker> = items

        fun addTicker(ticker: Ticker) {
            _items.add(ticker)
        }

        fun removeTicker(ticker: Ticker) {
            _items.remove(ticker)
        }
    }

    private class TestableWatchlistRepository(
        private val dataSource: FakeWatchlistLocalDataSource
    ) {
        private val _items = mutableListOf<Ticker>()
        val items: List<Ticker> get() = _items.toList()

        init {
            _items.addAll(dataSource.getTickers())
        }

        fun addTicker(ticker: Ticker) {
            dataSource.addTicker(ticker)
            _items.add(ticker)
        }

        fun removeTicker(ticker: Ticker) {
            dataSource.removeTicker(ticker)
            _items.remove(ticker)
        }
    }

    @Test
    fun initialItemsAreLoadedFromDataSource() {
        val dataSource = FakeWatchlistLocalDataSource()
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        dataSource.addTicker(ticker1)
        dataSource.addTicker(ticker2)

        val repository = TestableWatchlistRepository(dataSource)

        assertEquals(2, repository.items.size)
        assertTrue(repository.items.contains(ticker1))
        assertTrue(repository.items.contains(ticker2))
    }

    @Test
    fun addTickerAddsToBothRepositoryAndDataSource() {
        val dataSource = FakeWatchlistLocalDataSource()
        val repository = TestableWatchlistRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        repository.addTicker(ticker)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
        assertTrue(repository.items.contains(ticker))
        assertTrue(dataSource.items.contains(ticker))
    }

    @Test
    fun removeTickerRemovesFromBothRepositoryAndDataSource() {
        val dataSource = FakeWatchlistLocalDataSource()
        val repository = TestableWatchlistRepository(dataSource)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        repository.addTicker(ticker1)
        repository.addTicker(ticker2)

        repository.removeTicker(ticker1)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
        assertTrue(repository.items.contains(ticker2))
        assertTrue(!repository.items.contains(ticker1))
    }

    @Test
    fun addTickerAllowsDuplicates() {
        val dataSource = FakeWatchlistLocalDataSource()
        val repository = TestableWatchlistRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        repository.addTicker(ticker)
        repository.addTicker(ticker)

        assertEquals(2, repository.items.size)
        assertEquals(2, dataSource.items.size)
    }

    @Test
    fun removeTickerRemovesOnlyFirstOccurrence() {
        val dataSource = FakeWatchlistLocalDataSource()
        val repository = TestableWatchlistRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        repository.addTicker(ticker)
        repository.addTicker(ticker)

        repository.removeTicker(ticker)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
    }
}

