package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.feature.home.data.WatchlistRepository
import pl.deniotokiari.tickerwire.model.Ticker

@Factory
class AddTickerToWatchlistUseCase(
    private val watchlistRepository: WatchlistRepository,
) {
    suspend operator fun invoke(ticker: Ticker) = runCatching {
        watchlistRepository.addTicker(ticker)
    }
}
