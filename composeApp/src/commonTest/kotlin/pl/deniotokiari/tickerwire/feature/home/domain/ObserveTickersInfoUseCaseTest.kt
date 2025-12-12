package pl.deniotokiari.tickerwire.feature.home.domain

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveTickersInfoUseCaseTest {

    private class FakeConnectivityRepository {
        var isOnline: Boolean = true
    }

    private class FakeTickerRepository {
        private val infoCache = mutableMapOf<String, Map<Ticker, TickerData>>()

        fun setInfo(tickers: List<Ticker>, info: Map<Ticker, TickerData>, ttlSkip: Boolean) {
            val key = tickers.joinToString(",") { it.symbol } + "_$ttlSkip"
            infoCache[key] = info
        }

        fun info(tickers: List<Ticker>, ttlSkip: Boolean): Map<Ticker, TickerData> {
            val key = tickers.joinToString(",") { it.symbol } + "_$ttlSkip"
            return infoCache[key] ?: emptyMap()
        }
    }

    private class TestableObserveTickersInfoUseCase(
        private val tickerRepository: FakeTickerRepository,
        private val connectivityRepository: FakeConnectivityRepository
    ) {
        operator fun invoke(tickers: List<Ticker>): Map<Ticker, TickerData> {
            val cached = tickerRepository.info(tickers, ttlSkip = true)

            if (cached.isNotEmpty()) {
                return cached
            }

            return tickerRepository.info(tickers, ttlSkip = !connectivityRepository.isOnline)
        }
    }

    @Test
    fun invokeReturnsCachedInfoWhenAvailable() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        val useCase = TestableObserveTickersInfoUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val tickerData = TickerData(
            ticker = ticker,
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        tickerRepository.setInfo(listOf(ticker), mapOf(ticker to tickerData), ttlSkip = true)

        val result = useCase(listOf(ticker))

        assertEquals(1, result.size)
        assertEquals(tickerData, result[ticker])
    }

    @Test
    fun invokeReturnsEmptyMapWhenNoCacheAndOffline() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        connectivityRepository.isOnline = false
        val useCase = TestableObserveTickersInfoUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        val result = useCase(listOf(ticker))

        assertTrue(result.isEmpty())
    }

    @Test
    fun invokeReturnsCachedWhenAvailableEvenIfOnline() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        connectivityRepository.isOnline = true
        val useCase = TestableObserveTickersInfoUseCase(tickerRepository, connectivityRepository)
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")
        val cachedData = TickerData(
            ticker = ticker,
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        tickerRepository.setInfo(listOf(ticker), mapOf(ticker to cachedData), ttlSkip = true)

        val result = useCase(listOf(ticker))

        assertEquals(1, result.size)
        assertEquals(cachedData, result[ticker])
    }

    @Test
    fun invokeHandlesMultipleTickers() {
        val tickerRepository = FakeTickerRepository()
        val connectivityRepository = FakeConnectivityRepository()
        val useCase = TestableObserveTickersInfoUseCase(tickerRepository, connectivityRepository)
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val tickerData1 = TickerData(
            ticker = ticker1,
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )
        val tickerData2 = TickerData(
            ticker = ticker2,
            marketValueFormatted = "2800.00",
            deltaFormatted = "-15.00",
            percentFormatted = "-0.53%",
            currency = "USD"
        )

        tickerRepository.setInfo(
            listOf(ticker1, ticker2),
            mapOf(ticker1 to tickerData1, ticker2 to tickerData2),
            ttlSkip = true
        )

        val result = useCase(listOf(ticker1, ticker2))

        assertEquals(2, result.size)
        assertEquals(tickerData1, result[ticker1])
        assertEquals(tickerData2, result[ticker2])
    }
}

