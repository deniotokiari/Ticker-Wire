package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.ConnectivityRepository
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData

@Factory
class ObserveTickersInfoUseCase(
    private val tickerRepository: TickerRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    operator fun invoke(tickers: Flow<List<Ticker>>): Flow<Map<Ticker, TickerData>> = flow {
        tickers.collect { tickers ->
            val cached = tickerRepository.info(tickers, ttlSkip = true)

            if (cached.isNotEmpty()) {
                emit(cached)
            }

            emit(tickerRepository.info(tickers, ttlSkip = !connectivityRepository.isOnline()))
        }
    }
}
