package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository

@Factory
class ClearTickersDataUseCase(
    private val tickerRepository: TickerRepository,
) {
    operator fun invoke() {
        tickerRepository.clear()
    }
}
