package pl.deniotokiari.tickerwire.feature.home.presentation

import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

sealed interface HomeUiAction {
    data class OnAddTicker(val item: Ticker) : HomeUiAction
    data class OnNewsClick(val item: TickerNews) : HomeUiAction
    data class OnRemoveTicker(val item: Ticker) : HomeUiAction
    data class OnTickerClick(val item: Ticker) : HomeUiAction
    data object OnErrorMessageActionClick : HomeUiAction
    data object OnErrorMessageClose : HomeUiAction
    data object OnRefresh : HomeUiAction
    data object OnSearchClick : HomeUiAction
    data object OnThemeChangeClick : HomeUiAction
}
