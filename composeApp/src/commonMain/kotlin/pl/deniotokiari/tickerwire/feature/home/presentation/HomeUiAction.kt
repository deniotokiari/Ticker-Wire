package pl.deniotokiari.tickerwire.feature.home.presentation

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

sealed interface HomeUiAction {
    data object OnRefresh : HomeUiAction
    data object OnThemeChangeClick : HomeUiAction
    data object OnSearchClick : HomeUiAction
    data class OnRemoveTicker(val item: Ticker) : HomeUiAction
    data class OnAddTicker(val item: Ticker) : HomeUiAction
    data object OnErrorMessageClose : HomeUiAction
    data object OnErrorMessageActionClick : HomeUiAction
    data class OnNewsClick(val item: TickerNews) : HomeUiAction
}
