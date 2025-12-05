package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class ObserveTickersNewsUseCase(
    private val tickerRepository: TickerRepository,
) {
    operator fun invoke(tickers: Flow<List<Ticker>>): Flow<List<TickerNews>> =
        tickers.map { items ->
            runCatching {
                tickerRepository.news(items)
            }.getOrNull() ?: emptyList()
        }
}
