package pl.deniotokiari.tickerwire.feature.home.domain

import org.koin.core.annotation.Factory
import pl.deniotokiari.tickerwire.common.data.ConnectivityRepository
import pl.deniotokiari.tickerwire.common.data.TickerRepository

@Factory
class RefreshUseCase(
    private val tickerRepository: TickerRepository,
    private val connectivityRepository: ConnectivityRepository,
) {
    suspend operator fun invoke() {
        if (connectivityRepository.isOnline()) {
            tickerRepository.refresh()
        }
    }
}
