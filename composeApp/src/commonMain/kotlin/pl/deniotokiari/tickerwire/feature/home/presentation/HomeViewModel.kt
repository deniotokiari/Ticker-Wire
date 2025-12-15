package pl.deniotokiari.tickerwire.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import pl.deniotokiari.tickerwire.common.analytics.Analytics
import pl.deniotokiari.tickerwire.common.domain.ApplyDarkThemeUseCase
import pl.deniotokiari.tickerwire.common.domain.ApplyLightThemeUseCase
import pl.deniotokiari.tickerwire.common.domain.IsDarkThemeUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.AddTickerToWatchlistUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.ClearTickersDataUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.GetCachedTickerInfoUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.GetCachedTickerNewsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.GetTickerNewsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.GetTickersInfoUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.GetVisitedTickerNewsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.ObserveWatchlistItemsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.RemoveTickerFromWatchlistUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.SetTickerNewsItemVisitedUseCase
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.TickerNews

@KoinViewModel
class HomeViewModel(
    isDarkThemeUseCase: IsDarkThemeUseCase,
    private val analytics: Analytics,
    private val addTickerToWatchlistUseCase: AddTickerToWatchlistUseCase,
    private val applyDarkThemeUseCase: ApplyDarkThemeUseCase,
    private val applyLightThemeUseCase: ApplyLightThemeUseCase,
    private val clearTickersDataUseCase: ClearTickersDataUseCase,
    private val getCachedTickerInfoUseCase: GetCachedTickerInfoUseCase,
    private val getCachedTickerNewsUseCase: GetCachedTickerNewsUseCase,
    private val getTickerNewsUseCase: GetTickerNewsUseCase,
    private val getTickersInfoUseCase: GetTickersInfoUseCase,
    private val getVisitedTickerNewsUseCase: GetVisitedTickerNewsUseCase,
    private val observeWatchlistItemsUseCase: ObserveWatchlistItemsUseCase,
    private val removeTickerFromWatchlistUseCase: RemoveTickerFromWatchlistUseCase,
    private val setTickerNewsItemVisitedUseCase: SetTickerNewsItemVisitedUseCase,
) : ViewModel() {
    private val _uiState: MutableStateFlow<HomeUiState> =
        MutableStateFlow(HomeUiState(isDarkTheme = isDarkThemeUseCase()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        // Track screen view
        analytics.logScreenView("home")

        viewModelScope.launch {
            observeWatchlistItemsUseCase().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        tickers = items,
                        info = getCachedTickerInfoUseCase(items),
                        isRefreshing = true,
                        newsUiState = getCachedTickerNewsUseCase(items).let { news ->
                            if (news.isEmpty()) {
                                HomeUiState.NewsUiState.Loading
                            } else {
                                HomeUiState.NewsUiState.Content(news)
                            }
                        },
                    )
                }

                refreshTickersData(items)

                _uiState.update { state -> state.copy(isRefreshing = false) }
            }
        }
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.OnAddTicker -> handleOnAddTicker(action.item)
            HomeUiAction.OnRefresh -> handleOnRefresh()
            is HomeUiAction.OnRemoveTicker -> handleOnRemoveTicker(action.item)
            HomeUiAction.OnSearchClick -> handleOnSearchClick()
            HomeUiAction.OnThemeChangeClick -> handleOnThemeChangeClick()
            is HomeUiAction.OnNewsClick -> handleOnNewsClick(action.item)
            HomeUiAction.OnErrorMessageActionClick -> handleOnErrorMessageActionClick()
            HomeUiAction.OnErrorMessageClose -> handleOnErrorMessageClose()
        }
    }

    private fun handleOnErrorMessageActionClick() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    errorUiState = HomeUiState.ErrorUiState.None,
                    newsUiState = HomeUiState.NewsUiState.Loading,
                )
            }

            refreshTickersData(_uiState.value.tickers)
        }
    }

    private fun handleOnErrorMessageClose() {
        _uiState.update { state ->
            state.copy(errorUiState = HomeUiState.ErrorUiState.None)
        }
    }

    private fun handleOnNewsClick(item: TickerNews) {
        // Track news click
        analytics.logNewsClicked(item.ticker.symbol, item.url != null)

        viewModelScope.launch {
            launch {
                setTickerNewsItemVisitedUseCase(item)
                loadVisitedNews(_uiState.value.newsUiState.items)
            }

            item.url?.let { url -> _uiEvent.emit(HomeUiEvent.OpenNewsUri(url)) }
        }
    }

    private suspend fun loadNews(tickers: List<Ticker>): List<TickerNews> {
        return getTickerNewsUseCase(tickers).also { news ->
            _uiState.update { state ->
                state.copy(newsUiState = HomeUiState.NewsUiState.Content(news))
            }
        }
    }

    private suspend fun loadInfo(tickers: List<Ticker>) {
        _uiState.update { state ->
            state.copy(info = getTickersInfoUseCase(tickers))
        }
    }

    private suspend fun loadVisitedNews(news: List<TickerNews>) {
        _uiState.update { state ->
            state.copy(
                visitedNews = getVisitedTickerNewsUseCase(news),
            )
        }
    }

    private suspend fun refreshTickersData(tickers: List<Ticker>) {
        if (tickers.isEmpty()) return

        coroutineScope {
            launch {
                val news = loadNews(tickers)

                loadVisitedNews(news)
            }

            launch { loadInfo(tickers) }
        }
    }

    private fun handleOnRefresh() {
        if (_uiState.value.tickers.isEmpty()) return
        if (_uiState.value.isRefreshing) return
        if (refreshJob?.isActive == true) return

        // Track refresh event
        analytics.logRefreshTriggered()

        refreshJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isRefreshing = true)
            }

            clearTickersDataUseCase()
            refreshTickersData(_uiState.value.tickers)

            _uiState.update { state ->
                state.copy(isRefreshing = false)
            }
        }
    }

    private fun handleOnAddTicker(ticker: Ticker) {
        // Track ticker added
        analytics.logTickerAdded(ticker.symbol)

        viewModelScope.launch {
            addTickerToWatchlistUseCase(ticker)
        }
    }

    private fun handleOnRemoveTicker(ticker: Ticker) {
        // Track ticker removed
        analytics.logTickerRemoved(ticker.symbol)

        viewModelScope.launch {
            removeTickerFromWatchlistUseCase(ticker)
        }
    }

    private fun handleOnSearchClick() {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.NavigateToSearch)
        }
    }

    private fun handleOnThemeChangeClick() {
        viewModelScope.launch {
            val isDarkTheme = _uiState.value.isDarkTheme

            _uiState.update { state -> state.copy(isDarkTheme = !isDarkTheme) }

            // Track theme change
            val newTheme = if (isDarkTheme) "light" else "dark"
            analytics.logThemeChanged(newTheme)

            if (isDarkTheme) {
                applyLightThemeUseCase()
            } else {
                applyDarkThemeUseCase()
            }
        }
    }
}
