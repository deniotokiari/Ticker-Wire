package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData

@Factory
class GetCachedTickerInfoUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke(tickers: List<Ticker>): Map<Ticker, TickerData> {
        return tickerRepository.cachedInfo(tickers = tickers, ttlSkip = true)
    }
}
