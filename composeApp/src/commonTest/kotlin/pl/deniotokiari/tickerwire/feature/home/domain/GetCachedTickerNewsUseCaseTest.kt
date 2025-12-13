package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCachedTickerNewsUseCaseTest {

    private class FakeTickerRepository {
        var cachedNews: Map<String, List<TickerNews>> = emptyMap()

        fun cachedNews(tickers: List<Ticker>, ttlSkip: Boolean): List<TickerNews> {
            return tickers
                .mapNotNull { ticker -> cachedNews[ticker.symbol] }
                .flatten()
        }
    }

    private class TestableGetCachedTickerNewsUseCase(
        private val repository: FakeTickerRepository
    ) {
        operator fun invoke(tickers: List<Ticker>): List<TickerNews> {
            return repository.cachedNews(tickers, ttlSkip = true)
        }
    }

    @Test
    fun invokeReturnsCachedNewsForTickers() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerNewsUseCase(repository)

        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val news1 = TickerNews(ticker1, "Apple News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker2, "Google News 1", "Provider", "2024-01-15", 1705320001000L, null)

        repository.cachedNews = mapOf(
            "AAPL" to listOf(news1),
            "GOOGL" to listOf(news2)
        )

        val result = useCase(listOf(ticker1, ticker2))

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
    }

    @Test
    fun invokeReturnsEmptyListWhenNoCache() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")

        val result = useCase(listOf(ticker))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsOnlyMatchingTickers() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerNewsUseCase(repository)

        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val ticker3 = Ticker("MSFT", "Microsoft")
        val news1 = TickerNews(ticker1, "Apple News", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker2, "Google News", "Provider", "2024-01-15", 1705320001000L, null)
        val news3 = TickerNews(ticker3, "Microsoft News", "Provider", "2024-01-15", 1705320002000L, null)

        repository.cachedNews = mapOf(
            "AAPL" to listOf(news1),
            "GOOGL" to listOf(news2),
            "MSFT" to listOf(news3)
        )

        val result = useCase(listOf(ticker1, ticker3))

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news3))
        assertTrue(!result.contains(news2))
    }

    @Test
    fun invokeHandlesEmptyTickerList() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerNewsUseCase(repository)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsMultipleNewsForSameTicker() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerNewsUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")
        val news1 = TickerNews(ticker, "News 1", "Provider", "2024-01-15", 1705320000000L, null)
        val news2 = TickerNews(ticker, "News 2", "Provider", "2024-01-15", 1705320001000L, null)
        val news3 = TickerNews(ticker, "News 3", "Provider", "2024-01-15", 1705320002000L, null)

        repository.cachedNews = mapOf("AAPL" to listOf(news1, news2, news3))

        val result = useCase(listOf(ticker))

        assertEquals(3, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
        assertTrue(result.contains(news3))
    }
}

