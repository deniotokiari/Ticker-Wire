package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.feature.home.data.VisitedTickerNewsRepository
import pl.deniotokiari.tickerwire.model.TickerNews

@Factory
class SetTickerNewsItemVisitedUseCase(
    private val visitedTickerNewsRepository: VisitedTickerNewsRepository,
) {
    suspend operator fun invoke(item: TickerNews) {
        visitedTickerNewsRepository.addTickerNews(item)
    }
}
