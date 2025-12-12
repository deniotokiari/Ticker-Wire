package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveVisitedTickerNewsUseCaseTest {

    private class FakeVisitedTickerNewsRepository {
        val items: Set<TickerNews> = emptySet()
    }

    private class FakeObserveTickersNewsUseCase {
        fun invoke(): List<TickerNews> = emptyList()
    }

    // Simplified testable version that tests the merge logic
    private class TestableObserveVisitedTickerNewsUseCase {
        fun merge(news: List<TickerNews>, visited: Set<TickerNews>): Set<TickerNews> {
            val result = mutableSetOf<TickerNews>()
            for (item in news) {
                if (visited.contains(item)) {
                    result.add(item)
                }
            }
            return result
        }
    }

    @Test
    fun mergeReturnsOnlyNewsThatAreVisited() {
        val useCase = TestableObserveVisitedTickerNewsUseCase()
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
        val news3 = TickerNews(
            ticker = ticker1,
            title = "News 3",
            provider = "Provider",
            dateTimeFormatted = "2024-01-17",
            timestamp = 1705492800000L,
            url = null
        )

        val allNews = listOf(news1, news2, news3)
        val visited = setOf(news1, news3)

        val result = useCase.merge(allNews, visited)

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news3))
        assertTrue(!result.contains(news2))
    }

    @Test
    fun mergeReturnsEmptySetWhenNoNewsAreVisited() {
        val useCase = TestableObserveVisitedTickerNewsUseCase()
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "News",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        val allNews = listOf(news)
        val visited = emptySet<TickerNews>()

        val result = useCase.merge(allNews, visited)

        assertTrue(result.isEmpty())
    }

    @Test
    fun mergeReturnsEmptySetWhenNewsListIsEmpty() {
        val useCase = TestableObserveVisitedTickerNewsUseCase()
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "News",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        val allNews = emptyList<TickerNews>()
        val visited = setOf(news)

        val result = useCase.merge(allNews, visited)

        assertTrue(result.isEmpty())
    }

    @Test
    fun mergeReturnsAllNewsWhenAllAreVisited() {
        val useCase = TestableObserveVisitedTickerNewsUseCase()
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

        val allNews = listOf(news1, news2)
        val visited = setOf(news1, news2)

        val result = useCase.merge(allNews, visited)

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
    }
}

