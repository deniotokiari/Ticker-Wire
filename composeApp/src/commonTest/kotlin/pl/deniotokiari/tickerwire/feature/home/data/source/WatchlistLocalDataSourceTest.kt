package pl.deniotokiari.tickerwire.feature.home.data.source

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchlistLocalDataSourceTest {

    private class FakeKeyValueLocalDataSource {
        private val storage = mutableMapOf<String, Any>()

        fun <T> getValue(key: String, kSerializer: KSerializer<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return storage[key] as? T
        }

        fun <T> setValue(key: String, value: T, kSerializer: KSerializer<T>) {
            storage[key] = value as Any
        }
    }

    private class TestableWatchlistLocalDataSource(
        private val keyValueLocalDataSource: FakeKeyValueLocalDataSource
    ) {
        private val KEY_ITEMS = "items"

        fun addTicker(ticker: Ticker) {
            val tickers = getTickers()
            keyValueLocalDataSource.setValue(
                key = KEY_ITEMS,
                value = tickers + ticker,
                kSerializer = serializer<List<Ticker>>(),
            )
        }

        fun removeTicker(ticker: Ticker) {
            val tickers = getTickers()
            keyValueLocalDataSource.setValue(
                key = KEY_ITEMS,
                value = tickers - ticker,
                kSerializer = serializer<List<Ticker>>(),
            )
        }

        fun getTickers(): List<Ticker> {
            return keyValueLocalDataSource.getValue(
                key = KEY_ITEMS,
                kSerializer = serializer<List<Ticker>>(),
            ) ?: emptyList()
        }
    }

    private lateinit var keyValueLocalDataSource: FakeKeyValueLocalDataSource
    private lateinit var dataSource: TestableWatchlistLocalDataSource

    @BeforeTest
    fun setup() {
        keyValueLocalDataSource = FakeKeyValueLocalDataSource()
        dataSource = TestableWatchlistLocalDataSource(keyValueLocalDataSource)
    }

    @Test
    fun initialGetTickersReturnsEmptyList() {
        val tickers = dataSource.getTickers()

        assertTrue(tickers.isEmpty())
    }

    @Test
    fun addTickerAddsToTickers() {
        val ticker = Ticker("AAPL", "Apple Inc")

        dataSource.addTicker(ticker)

        val tickers = dataSource.getTickers()
        assertEquals(1, tickers.size)
        assertEquals(ticker, tickers[0])
    }

    @Test
    fun addTickerWithMultipleTickers() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val ticker3 = Ticker("MSFT", "Microsoft")

        dataSource.addTicker(ticker1)
        dataSource.addTicker(ticker2)
        dataSource.addTicker(ticker3)

        val tickers = dataSource.getTickers()
        assertEquals(3, tickers.size)
        assertTrue(tickers.contains(ticker1))
        assertTrue(tickers.contains(ticker2))
        assertTrue(tickers.contains(ticker3))
    }

    @Test
    fun removeTickerRemovesFromTickers() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")

        dataSource.addTicker(ticker1)
        dataSource.addTicker(ticker2)

        dataSource.removeTicker(ticker1)

        val tickers = dataSource.getTickers()
        assertEquals(1, tickers.size)
        assertTrue(tickers.contains(ticker2))
        assertFalse(tickers.contains(ticker1))
    }

    @Test
    fun removeTickerDoesNotFailWhenTickerNotPresent() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")

        dataSource.addTicker(ticker1)
        dataSource.removeTicker(ticker2)

        val tickers = dataSource.getTickers()
        assertEquals(1, tickers.size)
        assertTrue(tickers.contains(ticker1))
    }

    @Test
    fun addTickerAllowsDuplicates() {
        val ticker = Ticker("AAPL", "Apple Inc")

        dataSource.addTicker(ticker)
        dataSource.addTicker(ticker)

        val tickers = dataSource.getTickers()
        assertEquals(2, tickers.size)
        assertEquals(ticker, tickers[0])
        assertEquals(ticker, tickers[1])
    }

    @Test
    fun removeTickerRemovesOnlyFirstOccurrence() {
        val ticker = Ticker("AAPL", "Apple Inc")

        dataSource.addTicker(ticker)
        dataSource.addTicker(ticker)
        dataSource.addTicker(ticker)

        dataSource.removeTicker(ticker)

        val tickers = dataSource.getTickers()
        assertEquals(2, tickers.size)
        assertTrue(tickers.contains(ticker))
    }

    @Test
    fun getTickersReturnsPersistedData() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")

        dataSource.addTicker(ticker1)
        dataSource.addTicker(ticker2)

        val tickers1 = dataSource.getTickers()
        val tickers2 = dataSource.getTickers()

        assertEquals(tickers1, tickers2)
        assertEquals(2, tickers1.size)
    }

    @Test
    fun removeAllTickersReturnsEmptyList() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")

        dataSource.addTicker(ticker1)
        dataSource.addTicker(ticker2)

        dataSource.removeTicker(ticker1)
        dataSource.removeTicker(ticker2)

        val tickers = dataSource.getTickers()
        assertTrue(tickers.isEmpty())
    }
}

