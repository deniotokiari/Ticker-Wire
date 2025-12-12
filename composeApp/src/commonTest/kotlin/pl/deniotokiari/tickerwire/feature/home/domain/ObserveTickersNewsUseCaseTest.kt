package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveTickersNewsUseCaseTest {

    private class FakeConnectivityRepository {
        var isOnline: Boolean = true
    }

    private class FakeTickerRepository {
        private val newsCache = mutableMapOf<String, List<TickerNews>>()

        fun setNews(tickers: List<Ticker>, news: List<TickerNews>, ttlSkip: Boolean) {
            val key = tickers.joinToString(",") { it.symbol } + "_$ttlSkip"
            newsCache[key] = news
        }

        fun news(tickers: List<Ticker>, ttlSkip: Boolean): List<TickerNews> {
            val key = tickers.joinToString(",") { it.symbol } + "_$ttlSkip"
            return newsCache[key] ?: emptyList()
        }
    }

    private class TestableObserveTickersNewsUseCase(
        private val tickerRepository: FakeTickerRepository,
        private val connectivityRepository: FakeConnectivityRepository
    ) {
        operator fun invoke(tickers: List<Ticker>): List<TickerNews> {
            val cached = tickerRepository.news(tickers, ttlSkip = true)

            if (cached.isNotEmpty()) {
                return cached
            }

            return tickerRepository.news(tickers, ttlSkip = !connectivityRepository.isOnline)
        }
    }

    @Test
    fun invokeReturnsCachedNewsWhenAvailable() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        val useCase = TestableObserveTickersNewsUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val news = TickerNews(
            ticker = ticker,
            title = "Apple Reports Record Earnings",
            provider = "TechNews",
            dateTimeFormatted = "2024-01-15 12:00:00",
            timestamp = 1705320000000L,
            url = "https://example.com/news/apple"
        )

        tickerRepository.setNews(listOf(ticker), listOf(news), ttlSkip = true)

        val result = useCase(listOf(ticker))

        assertEquals(1, result.size)
        assertEquals(news, result[0])
    }

    @Test
    fun invokeReturnsEmptyListWhenNoCacheAndOffline() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        connectivityRepository.isOnline = false
        val useCase = TestableObserveTickersNewsUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        val result = useCase(listOf(ticker))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsCachedWhenAvailableEvenIfOnline() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        connectivityRepository.isOnline = true
        val useCase = TestableObserveTickersNewsUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val cachedNews = TickerNews(
            ticker = ticker,
            title = "Old News",
            provider = "TechNews",
            dateTimeFormatted = "2024-01-15 12:00:00",
            timestamp = 1705320000000L,
            url = "https://example.com/news/old"
        )

        tickerRepository.setNews(listOf(ticker), listOf(cachedNews), ttlSkip = true)

        val result = useCase(listOf(ticker))

        assertEquals(1, result.size)
        assertEquals(cachedNews, result[0])
    }

    @Test
    fun invokeHandlesMultipleTickers() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        val useCase = TestableObserveTickersNewsUseCase(tickerRepository, connectivityRepository)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val news1 = TickerNews(
            ticker = ticker1,
            title = "Apple News",
            provider = "TechNews",
            dateTimeFormatted = "2024-01-15 12:00:00",
            timestamp = 1705320000000L,
            url = "https://example.com/news/apple"
        )
        val news2 = TickerNews(
            ticker = ticker2,
            title = "Google News",
            provider = "TechNews",
            dateTimeFormatted = "2024-01-15 13:00:00",
            timestamp = 1705323600000L,
            url = "https://example.com/news/google"
        )

        tickerRepository.setNews(
            listOf(ticker1, ticker2),
            listOf(news1, news2),
            ttlSkip = true
        )

        val result = useCase(listOf(ticker1, ticker2))

        assertEquals(2, result.size)
        assertTrue(result.contains(news1))
        assertTrue(result.contains(news2))
    }

    @Test
    fun invokeHandlesEmptyTickerList() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        val useCase = TestableObserveTickersNewsUseCase(tickerRepository, connectivityRepository)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }
}

