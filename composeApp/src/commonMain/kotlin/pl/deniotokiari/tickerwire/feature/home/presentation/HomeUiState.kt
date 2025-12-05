package pl.deniotokiari.tickerwire.feature.home.presentation

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import pl.deniotokiari.tickerwire.model.TickerNews

data class HomeUiState(
    val tickers: List<Ticker> = emptyList(),
    val info: Map<Ticker, TickerData> = emptyMap(),
    val isRefreshing: Boolean = false,
    val isDarkTheme: Boolean = false,
    val newsUiState: NewsUiState = NewsUiState.Loading,
    val errorUiState: ErrorUiState = ErrorUiState.None,
) {
    sealed interface NewsUiState {
        data object Loading : NewsUiState
        data class Content(val news: List<TickerNews>) : NewsUiState
    }

    sealed interface ErrorUiState {
        data object None : ErrorUiState
        data object Error : ErrorUiState
    }
}
