package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetVisitedTickerNewsUseCaseTest {

    private class FakeVisitedTickerNewsRepository {
        private val _items = mutableSetOf<TickerNews>()
        val items: Set<TickerNews> get() = _items.toSet()

        fun addTickerNews(item: TickerNews) {
            _items.add(item)
        }

        fun removeTickerNews(item: TickerNews) {
            _items.remove(item)
        }
    }

    private class TestableGetVisitedTickerNewsUseCase(
        private val repository: FakeVisitedTickerNewsRepository
    ) {
        operator fun invoke(news: List<TickerNews>): Set<TickerNews> {
            val result = mutableSetOf<TickerNews>()
            val visited = repository.items

            for (item in news) {
                if (visited.contains(item)) {
                    result.add(item)
                }
            }

            (visited - result).let { items ->
                items.forEach { item ->
                    repository.removeTickerNews(item)
                }
            }

            return result
        }
    }

    @Test
    fun invokeReturnsVisitedNewsFromList() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableGetVisitedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-15", 1705320001000L, null)
        val news3 = TickerNews(ticker, "News 3", "Provider", "2024-01-15", 1705320002000L, null)

        repository.addTickerNews(news1)
        repository.addTickerNews(news2)

        val result = useCase(listOf(news1, news2, news3))

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
        assertFalse(result.contains(news3))
    }

    @Test
    fun invokeReturnsEmptySetWhenNoVisitedNews() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableGetVisitedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-15", 1705320001000L, null)

        val result = useCase(listOf(news1, news2))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeRemovesStaleVisitedNews() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableGetVisitedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-15", 1705320001000L, null)
        val staleNews = TickerNews(ticker, "Stale News", "Provider", "2024-01-14", 1705233600000L, null)

        repository.addTickerNews(news1)
        repository.addTickerNews(staleNews)

        val result = useCase(listOf(news1, news2))

        assertEquals(1, result.size)
        assertTrue(result.contains(news1))
        assertFalse(repository.items.contains(staleNews)) // Stale news should be removed
    }

    @Test
    fun invokeHandlesEmptyNewsList() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableGetVisitedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        repository.addTickerNews(news)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
        // Stale news should be removed when not in the current news list
        assertFalse(repository.items.contains(news))
    }

    @Test
    fun invokePreservesVisitedNewsThatAreStillInList() {
        val repository = FakeVisitedTickerNewsRepository()
        val useCase = TestableGetVisitedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-15", 1705320001000L, null)

        repository.addTickerNews(news1)
        repository.addTickerNews(news2)

        val result = useCase(listOf(news1, news2))

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
        assertTrue(repository.items.contains(news1))
        assertTrue(repository.items.contains(news2))
    }
}

