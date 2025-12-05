package pl.deniotokiari.tickerwire.feature.search.presentation

import pl.deniotokiari.tickerwire.model.Ticker

sealed interface SearchUiState {
    val query: String

    data class Idle(override val query: String = "") : SearchUiState

    data class Error(override val query: String) : SearchUiState

    data class Empty(override val query: String) : SearchUiState

    data class Loading(override val query: String) : SearchUiState

    data class Content(
        override val query: String,
        val items: List<Ticker>,
    ) : SearchUiState
}
