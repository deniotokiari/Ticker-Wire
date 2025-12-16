package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertTrue

class SetTickerNewsItemVisitedUseCaseTest {

    private class FakeVisitedTickerNewsRepository {
        private val _items = mutableSetOf<TickerNews>()
        val items: Set<TickerNews> get() = _items.toSet()

        fun addTickerNews(item: TickerNews) {
            _items.add(item)
        }
    }

    private class TestableSetTickerNewsItemVisitedUseCase(
        private val repository: FakeVisitedTickerNewsRepository
    ) {
        operator fun invoke(item: TickerNews) {
            repository.addTickerNews(item)
        }
    }

    @Test
    fun invokeAddsTickerNewsToRepository() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableSetTickerNewsItemVisitedUseCase(repository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Test News",
            provider = "Test Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        useCase(news)

        assertTrue(repository.items.contains(news))
    }

    @Test
    fun invokeAddsMultipleTickerNews() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableSetTickerNewsItemVisitedUseCase(repository)
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

        useCase(news1)
        useCase(news2)

        assertTrue(repository.items.contains(news1))
        assertTrue(repository.items.contains(news2))
        assertTrue(repository.items.size == 2)
    }

    @Test
    fun invokeWithNewsWithUrl() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableSetTickerNewsItemVisitedUseCase(repository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Test News",
            provider = "Test Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com/news"
        )

        useCase(news)

        assertTrue(repository.items.contains(news))
    }
}

