package pl.deniotokiari.tickerwire.feature.home.domain

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.feature.home.data.VisitedTickerNewsRepository
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class ObserveVisitedTickerNewsUseCase(
    private val visitedTickerNewsRepository: VisitedTickerNewsRepository,
    private val observeTickersNewsUseCase: ObserveTickersNewsUseCase,
) {
    operator fun invoke(): Flow<Set<TickerNews>> = channelFlow {
        var lastNews: List<TickerNews>? = null
        var lastVisited: Set<TickerNews>? = null

        suspend fun merge() {
            val news = lastNews ?: return
            val visited = lastVisited ?: return
            val result = mutableSetOf<TickerNews>()

            for (item in news) {
                if (visited.contains(item)) {
                    result.add(item)
                }
            }

            channel.send(result)
        }

        launch {
            visitedTickerNewsRepository.items.collect { items ->
                lastVisited = items

                merge()
            }
        }

        launch {
            observeTickersNewsUseCase().collect { items ->
                lastNews = items

                merge()
            }
        }
    }
}
