package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.feature.home.data.WatchlistRepository
import pl.deniotokiari.tickerwire.model.Ticker

@Factory
class ObserveWatchlistItemsUseCase(
    private val watchlistRepository: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<Ticker>> = watchlistRepository.items
}
