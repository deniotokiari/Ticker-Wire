package pl.deniotokiari.tickerwire.feature.search.presentation

import pl.deniotokiari.tickerwire.model.Ticker

sealed interface SearchUiEvent {
    data object NavigateBack : SearchUiEvent
    data class NavigateBackWithSelectedSearchItem(
        val item: Ticker,
    ) : SearchUiEvent
}
