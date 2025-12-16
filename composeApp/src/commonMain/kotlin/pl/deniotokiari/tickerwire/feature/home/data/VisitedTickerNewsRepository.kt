package pl.deniotokiari.tickerwire.feature.home.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.feature.home.data.source.VisitedTickerNewsLocalDataSource
import pl.deniotokiari.tickerwire.model.TickerNews

@Single
class VisitedTickerNewsRepository(
    private val visitedTickerNewsLocalDataSource: VisitedTickerNewsLocalDataSource,
) {
    private val _items = MutableStateFlow(emptySet<TickerNews>())
    val items: StateFlow<Set<TickerNews>> = _items.asStateFlow()

    init {
        _items.update {
            visitedTickerNewsLocalDataSource.items
        }
    }

    suspend fun addTickerNews(item: TickerNews) {
        visitedTickerNewsLocalDataSource.addTickerNews(item)

        _items.emit(_items.value + item)
    }

    suspend fun removeTickerNews(item: TickerNews) {
        visitedTickerNewsLocalDataSource.removeTickerNews(item)

        _items.emit(_items.value - item)
    }
}
