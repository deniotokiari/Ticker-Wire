package pl.deniotokiari.tickerwire.feature.home.presentation

import pl.deniotokiari.tickerwire.model.Ticker

sealed interface HomeUiAction {
    data object OnRefresh : HomeUiAction
    data object OnThemeChangeClick : HomeUiAction
    data object OnSearchClick : HomeUiAction
    data class OnRemoveTicker(val item: Ticker) : HomeUiAction
    data class OnAddTicker(val item: Ticker) : HomeUiAction
    data class OnNewsClick(val ticker: String, val url: String?) : HomeUiAction
}
