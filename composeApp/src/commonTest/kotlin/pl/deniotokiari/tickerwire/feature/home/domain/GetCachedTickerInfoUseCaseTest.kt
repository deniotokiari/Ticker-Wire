package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCachedTickerInfoUseCaseTest {

    private class FakeTickerRepository {
        var cachedInfo: Map<Ticker, TickerData> = emptyMap()

        fun cachedInfo(tickers: List<Ticker>, ttlSkip: Boolean): Map<Ticker, TickerData> {
            return cachedInfo.filterKeys { it in tickers }
        }
    }

    private class TestableGetCachedTickerInfoUseCase(
        private val repository: FakeTickerRepository
    ) {
        operator fun invoke(tickers: List<Ticker>): Map<Ticker, TickerData> {
            return repository.cachedInfo(tickers, ttlSkip = true)
        }
    }

    @Test
    fun invokeReturnsCachedInfoForTickers() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerInfoUseCase(repository)

        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val data1 = TickerData(ticker1, "150.00", "+2.00", "+1.35%", "USD")
        val data2 = TickerData(ticker2, "2800.00", "-15.00", "-0.53%", "USD")

        repository.cachedInfo = mapOf(ticker1 to data1, ticker2 to data2)

        val result = useCase(listOf(ticker1, ticker2))

        assertEquals(2, result.size)
        assertEquals(data1, result[ticker1])
        assertEquals(data2, result[ticker2])
    }

    @Test
    fun invokeReturnsEmptyMapWhenNoCache() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerInfoUseCase(repository)

        val ticker = Ticker("AAPL", "Apple Inc")

        val result = useCase(listOf(ticker))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsOnlyMatchingTickers() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerInfoUseCase(repository)

        val ticker1 = Ticker("AAPL", "Apple Inc")
        val ticker2 = Ticker("GOOGL", "Alphabet Inc")
        val ticker3 = Ticker("MSFT", "Microsoft")
        val data1 = TickerData(ticker1, "150.00", "+2.00", "+1.35%", "USD")
        val data2 = TickerData(ticker2, "2800.00", "-15.00", "-0.53%", "USD")
        val data3 = TickerData(ticker3, "400.00", "+5.00", "+1.25%", "USD")

        repository.cachedInfo = mapOf(ticker1 to data1, ticker2 to data2, ticker3 to data3)

        val result = useCase(listOf(ticker1, ticker3))

        assertEquals(2, result.size)
        assertEquals(data1, result[ticker1])
        assertEquals(data3, result[ticker3])
        assertTrue(result[ticker2] == null)
    }

    @Test
    fun invokeHandlesEmptyTickerList() {
        val repository = FakeTickerRepository()
        val useCase = TestableGetCachedTickerInfoUseCase(repository)

        val result = useCase(emptyList())

        assertTrue(result.isEmpty())
    }
}

