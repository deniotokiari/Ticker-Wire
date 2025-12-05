package pl.deniotokiari.tickerwire.feature.home.presentation

sealed interface HomeUiEvent {
    data object NavigateToSearch : HomeUiEvent
    data class OpenNewsUri(val uri: String) : HomeUiEvent
}
