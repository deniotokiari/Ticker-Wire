package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.ConnectivityRepository
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class ObserveTickersNewsUseCase(
    private val tickerRepository: TickerRepository,
    private val connectivityRepository: ConnectivityRepository,
    private val observeWatchlistItemsUseCase: ObserveWatchlistItemsUseCase,
) {
    operator fun invoke(): Flow<List<TickerNews>> = flow {
        observeWatchlistItemsUseCase().collect { items ->
            val cached = tickerRepository.news(items, ttlSkip = true)

            if (cached.isNotEmpty()) {
                emit(cached)
            }

            emit(tickerRepository.news(items, ttlSkip = !connectivityRepository.isOnline()))
        }
    }
}
