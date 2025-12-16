package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.feature.home.data.VisitedTickerNewsRepository
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class GetVisitedTickerNewsUseCase(
    private val visitedTickerNewsRepository: VisitedTickerNewsRepository,
) {
    suspend operator fun invoke(news: List<TickerNews>): Set<TickerNews> {
        val result = mutableSetOf<TickerNews>()
        val visited = visitedTickerNewsRepository.items.value

        for (item in news) {
            if (visited.contains(item)) {
                result.add(item)
            }
        }

        (visited - result).let { items ->
            items.forEach { item ->
                visitedTickerNewsRepository.removeTickerNews(item)
            }
        }

        return result
    }
}
