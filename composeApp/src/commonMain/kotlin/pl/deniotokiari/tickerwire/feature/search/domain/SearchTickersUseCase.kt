package pl.deniotokiari.tickerwire.feature.search.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.ConnectivityRepository
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker

@Factory
class SearchTickersUseCase(
    private val tickerRepository: TickerRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke(query: String): Result<List<Ticker>> = runCatching {
        tickerRepository.search(query, ttlSkip = !connectivityRepository.isOnline())
    }
}
