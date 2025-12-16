package pl.deniotokiari.tickerwire.feature.home.presentation

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerData
import pl.deniotokiari.tickerwire.model.TickerNews

data class HomeUiState(
    val errorUiState: ErrorUiState = ErrorUiState.None,
    val info: Map<Ticker, TickerData> = emptyMap(),
    val isDarkTheme: Boolean = false,
    val isRefreshing: Boolean = false,
    val newsUiState: NewsUiState = NewsUiState.Loading,
    val selectedTickers: Set<Ticker> = emptySet(),
    val tickers: List<Ticker> = emptyList(),
    val visitedNews: Set<TickerNews> = emptySet(),
) {
    val filteredNews: List<TickerNews>
        get() = if (selectedTickers.isEmpty()) {
            newsUiState.items
        } else {
            newsUiState.items.filter { item ->
                selectedTickers.contains(item.ticker)
            }
        }

    sealed interface NewsUiState {
        data object Loading : NewsUiState
        data class Content(val news: List<TickerNews>) : NewsUiState

        val items: List<TickerNews>
            get() = when (this) {
                is Content -> news
                Loading -> emptyList()
            }
    }

    sealed interface ErrorUiState {
        data object None : ErrorUiState
        data object Error : ErrorUiState
    }
}
