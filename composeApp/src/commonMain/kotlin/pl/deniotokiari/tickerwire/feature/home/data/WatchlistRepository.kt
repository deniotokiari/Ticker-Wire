package pl.deniotokiari.tickerwire.feature.home.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.feature.home.data.source.WatchlistLocalDataSource
import pl.deniotokiari.tickerwire.model.Ticker

@Single
class WatchlistRepository(
    private val watchlistLocalDataSource: WatchlistLocalDataSource,
) {
    private val _items = MutableStateFlow(emptyList<Ticker>())
    val items: StateFlow<List<Ticker>> = _items.asStateFlow()

    init {
        _items.update {
            watchlistLocalDataSource.getTickers()
        }
    }

    suspend fun addTicker(ticker: Ticker) {
        watchlistLocalDataSource.addTicker(ticker)

        _items.emit(_items.value + ticker)
    }

    suspend fun removeTicker(ticker: Ticker) {
        watchlistLocalDataSource.removeTicker(ticker)

        _items.emit(_items.value - ticker)
    }
}
