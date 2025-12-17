package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData

@Factory
class GetTickersInfoUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke(tickers: List<Ticker>): Result<Map<Ticker, TickerData>> =
        runCatching {
            tickerRepository.info(tickers)
        }
}
