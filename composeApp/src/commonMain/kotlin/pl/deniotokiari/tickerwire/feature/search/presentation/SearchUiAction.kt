package pl.deniotokiari.tickerwire.feature.search.presentation

import pl.deniotokiari.tickerwire.model.Ticker

sealed interface SearchUiAction {
    data class OnQueryChanged(val query: String) : SearchUiAction
    data object OnBackClicked : SearchUiAction
    data object OnErrorMessageClosed : SearchUiAction
    data object OnErrorMessageActionClicked : SearchUiAction
    data class OnSearchItemClicked(val item: Ticker) : SearchUiAction
}
