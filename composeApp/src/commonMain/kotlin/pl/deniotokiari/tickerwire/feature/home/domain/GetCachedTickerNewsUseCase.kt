package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class GetCachedTickerNewsUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke(tickers: List<Ticker>): List<TickerNews> {
        return tickerRepository.cachedNews(tickers = tickers, ttlSkip = true)
    }
}
