package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class GetTickerNewsUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke(tickers: List<Ticker>): Result<List<TickerNews>> = runCatching {
        tickerRepository.news(tickers)
    }
}
