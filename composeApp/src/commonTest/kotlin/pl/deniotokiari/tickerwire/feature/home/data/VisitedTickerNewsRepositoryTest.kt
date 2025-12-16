package pl.deniotokiari.tickerwire.feature.home.data

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisitedTickerNewsRepositoryTest {

    private class FakeVisitedTickerNewsLocalDataSource {
        private val _items = mutableSetOf<TickerNews>()
        val items: Set<TickerNews> get() = _items.toSet()

        fun addTickerNews(item: TickerNews) {
            _items.add(item)
        }

        fun removeTickerNews(item: TickerNews) {
            _items.remove(item)
        }
    }

    private class TestableVisitedTickerNewsRepository(
        private val dataSource: FakeVisitedTickerNewsLocalDataSource
    ) {
        private val _items = mutableSetOf<TickerNews>()
        val items: Set<TickerNews> get() = _items.toSet()

        init {
            _items.addAll(dataSource.items)
        }

        fun addTickerNews(item: TickerNews) {
            dataSource.addTickerNews(item)
            _items.add(item)
        }

        fun removeTickerNews(item: TickerNews) {
            dataSource.removeTickerNews(item)
            _items.remove(item)
        }
    }

    @Test
    fun initialItemsAreLoadedFromDataSource() {
        val dataSource = FakeVisitedTickerNewsLocalDataSource()
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val news1 = TickerNews(
            ticker = ticker1,
            title = "News 1",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )
        val news2 = TickerNews(
            ticker = ticker2,
            title = "News 2",
            provider = "Provider",
            dateTimeFormatted = "2024-01-16",
            timestamp = 1705406400000L,
            url = null
        )
        dataSource.addTickerNews(news1)
        dataSource.addTickerNews(news2)

        val repository = TestableVisitedTickerNewsRepository(dataSource)

        assertEquals(2, repository.items.size)
        assertTrue(repository.items.contains(news1))
        assertTrue(repository.items.contains(news2))
    }

    @Test
    fun addTickerNewsAddsToBothRepositoryAndDataSource() {
        val dataSource = FakeVisitedTickerNewsLocalDataSource()
        val repository = TestableVisitedTickerNewsRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Test News",
            provider = "Test Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        repository.addTickerNews(news)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
        assertTrue(repository.items.contains(news))
        assertTrue(dataSource.items.contains(news))
    }

    @Test
    fun removeTickerNewsRemovesFromBothRepositoryAndDataSource() {
        val dataSource = FakeVisitedTickerNewsLocalDataSource()
        val repository = TestableVisitedTickerNewsRepository(dataSource)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val news1 = TickerNews(
            ticker = ticker1,
            title = "News 1",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )
        val news2 = TickerNews(
            ticker = ticker2,
            title = "News 2",
            provider = "Provider",
            dateTimeFormatted = "2024-01-16",
            timestamp = 1705406400000L,
            url = null
        )
        repository.addTickerNews(news1)
        repository.addTickerNews(news2)

        repository.removeTickerNews(news1)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
        assertTrue(repository.items.contains(news2))
        assertFalse(repository.items.contains(news1))
    }

    @Test
    fun addTickerNewsDoesNotAllowDuplicates() {
        val dataSource = FakeVisitedTickerNewsLocalDataSource()
        val repository = TestableVisitedTickerNewsRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Test News",
            provider = "Test Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        repository.addTickerNews(news)
        repository.addTickerNews(news)

        assertEquals(1, repository.items.size)
        assertEquals(1, dataSource.items.size)
    }

    @Test
    fun addTickerNewsWithUrl() {
        val dataSource = FakeVisitedTickerNewsLocalDataSource()
        val repository = TestableVisitedTickerNewsRepository(dataSource)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Test News",
            provider = "Test Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com/news"
        )

        repository.addTickerNews(news)

        assertTrue(repository.items.contains(news))
        assertEquals("https://example.com/news", repository.items.first().url)
    }
}

