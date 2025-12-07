package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData

@Factory
class ObserveTickersInfoUseCase(
    private val tickerRepository: TickerRepository,
) {
    operator fun invoke(tickers: Flow<List<Ticker>>): Flow<Map<Ticker, TickerData>> =
        tickers.mapNotNull { tickers ->
            tickerRepository.info(tickers)
        }
}
