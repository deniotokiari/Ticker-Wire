package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.TickerRepository

@Factory
class RefreshUseCase(
    private val tickerRepository: TickerRepository,
) {
    suspend operator fun invoke() {
        tickerRepository.refresh()
    }
}
