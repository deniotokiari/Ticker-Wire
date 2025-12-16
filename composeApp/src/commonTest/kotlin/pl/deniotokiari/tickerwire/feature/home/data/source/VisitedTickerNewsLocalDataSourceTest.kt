package pl.deniotokiari.tickerwire.feature.home.data.source

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisitedTickerNewsLocalDataSourceTest {

    private class FakeKeyValueLocalDataSource {
        private val storage = mutableMapOf<String, Any>()

        fun <T> getValue(key: String, kSerializer: KSerializer<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return storage[key] as? T
        }

        fun <T> setValue(key: String, value: T, kSerializer: KSerializer<T>) {
            storage[key] = value as Any
        }

        fun <T> removeValue(key: String, kSerializer: KSerializer<T>) {
            storage.remove(key)
        }
    }

    private class TestableVisitedTickerNewsLocalDataSource(
        private val keyValueLocalDataSource: FakeKeyValueLocalDataSource
    ) {
        private val KEY_ITEMS = "items"

        val items: Set<TickerNews>
            get() = keyValueLocalDataSource.getValue(
                key = KEY_ITEMS,
                kSerializer = serializer<Set<TickerNews>>(),
            ) ?: emptySet()

        fun addTickerNews(item: TickerNews) {
            keyValueLocalDataSource.setValue(
                key = KEY_ITEMS,
                value = items + item,
                kSerializer = serializer<Set<TickerNews>>(),
            )
        }

        fun removeTickerNews(item: TickerNews) {
            keyValueLocalDataSource.setValue(
                key = KEY_ITEMS,
                value = items - item,
                kSerializer = serializer<Set<TickerNews>>(),
            )
        }
    }

    private lateinit var keyValueLocalDataSource: FakeKeyValueLocalDataSource
    private lateinit var dataSource: TestableVisitedTickerNewsLocalDataSource

    @BeforeTest
    fun setup() {
        keyValueLocalDataSource = FakeKeyValueLocalDataSource()
        dataSource = TestableVisitedTickerNewsLocalDataSource(keyValueLocalDataSource)
    }

    @Test
    fun initialItemsReturnsEmptySet() {
        val items = dataSource.items

        assertTrue(items.isEmpty())
    }

    @Test
    fun addTickerNewsAddsToItems() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news = TickerNews(ticker, "Test News", "Provider", "2024-01-15", 1705320000000L, null)

        dataSource.addTickerNews(news)

        assertEquals(1, dataSource.items.size)
        assertTrue(dataSource.items.contains(news))
    }

    @Test
    fun addTickerNewsWithMultipleItems() {
        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val news1 = TickerNews(ticker1, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker2, "News 2", "Provider", "2024-01-16", 1705406400000L, null)

        dataSource.addTickerNews(news1)
        dataSource.addTickerNews(news2)

        assertEquals(2, dataSource.items.size)
        assertTrue(dataSource.items.contains(news1))
        assertTrue(dataSource.items.contains(news2))
    }

    @Test
    fun removeTickerNewsRemovesFromItems() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-16", 1705406400000L, null)

        dataSource.addTickerNews(news1)
        dataSource.addTickerNews(news2)

        dataSource.removeTickerNews(news1)

        assertEquals(1, dataSource.items.size)
        assertTrue(dataSource.items.contains(news2))
        assertFalse(dataSource.items.contains(news1))
    }

    @Test
    fun removeTickerNewsDoesNotFailWhenItemNotPresent() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-16", 1705406400000L, null)

        dataSource.addTickerNews(news1)
        dataSource.removeTickerNews(news2)

        assertEquals(1, dataSource.items.size)
        assertTrue(dataSource.items.contains(news1))
    }

    @Test
    fun addTickerNewsDoesNotAllowDuplicates() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news = TickerNews(ticker, "Test News", "Provider", "2024-01-15", 1705320000000L, null)

        dataSource.addTickerNews(news)
        dataSource.addTickerNews(news)

        assertEquals(1, dataSource.items.size)
    }

    @Test
    fun addTickerNewsWithUrl() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news = TickerNews(
            ticker,
            "Test News",
            "Provider",
            "2024-01-15",
            1705320000000L,
            "https://example.com/news"
        )

        dataSource.addTickerNews(news)

        assertEquals(1, dataSource.items.size)
        assertEquals("https://example.com/news", dataSource.items.first().url)
    }

    @Test
    fun itemsPersistAcrossMultipleCalls() {
        val ticker = Ticker("AAPL", "Apple Inc")
        val news = TickerNews(ticker, "Test News", "Provider", "2024-01-15", 1705320000000L, null)

        dataSource.addTickerNews(news)

        val items1 = dataSource.items
        val items2 = dataSource.items

        assertEquals(items1, items2)
        assertEquals(1, items1.size)
    }
}

