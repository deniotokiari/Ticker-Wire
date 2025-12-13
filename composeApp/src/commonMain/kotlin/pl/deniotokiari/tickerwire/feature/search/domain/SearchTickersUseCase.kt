package pl.deniotokiari.tickerwire.feature.search.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository
import pl.deniotokiari.tickerwire.model.Ticker

@Factory
class SearchTickersUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke(query: String): Result<List<Ticker>> = runCatching {
        tickerRepository.search(query)
    }
}
