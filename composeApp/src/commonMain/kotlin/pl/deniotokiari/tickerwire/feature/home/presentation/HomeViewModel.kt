package pl.deniotokiari.tickerwire.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import pl.deniotokiari.tickerwire.common.analytics.Analytics
import pl.deniotokiari.tickerwire.common.domain.ApplyDarkThemeUseCase
import pl.deniotokiari.tickerwire.common.domain.ApplyLightThemeUseCase
import pl.deniotokiari.tickerwire.common.domain.IsDarkThemeUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.AddTickerToWatchlistUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.ObserveTickersInfoUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.ObserveTickersNewsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.ObserveWatchlistItemsUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.RefreshUseCase
import pl.deniotokiari.tickerwire.feature.home.domain.RemoveTickerFromWatchlistUseCase
import pl.deniotokiari.tickerwire.model.Ticker

private const val REFRESH_DELAY = 1_000L

@KoinViewModel
class HomeViewModel(
    isDarkThemeUseCase: IsDarkThemeUseCase,
    private val analytics: Analytics,
    private val applyDarkThemeUseCase: ApplyDarkThemeUseCase,
    private val applyLightThemeUseCase: ApplyLightThemeUseCase,
    private val addTickerToWatchlistUseCase: AddTickerToWatchlistUseCase,
    private val removeTickerFromWatchlistUseCase: RemoveTickerFromWatchlistUseCase,
    private val observeWatchlistItemsUseCase: ObserveWatchlistItemsUseCase,
    private val observeTickersInfoUseCase: ObserveTickersInfoUseCase,
    private val observeTickersNewsUseCase: ObserveTickersNewsUseCase,
    private val refreshUseCase: RefreshUseCase,
) : ViewModel() {
    private val _uiState: MutableStateFlow<HomeUiState> =
        MutableStateFlow(HomeUiState(isDarkTheme = isDarkThemeUseCase()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent: SharedFlow<HomeUiEvent> = _uiEvent.asSharedFlow()

    private var infoUpdatesJob: Job? = null
    private var newsUpdatesJob: Job? = null

    init {
        // Track screen view
        analytics.logScreenView("home")

        viewModelScope.launch {
            observeWatchlistItemsUseCase().collect { items ->
                _uiState.update { state ->
                    state.copy(tickers = items)
                }
            }
        }

        subscribeForUpdates()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            is HomeUiAction.OnAddTicker -> handleOnAddTicker(action.item)
            HomeUiAction.OnRefresh -> handleOnRefresh()
            is HomeUiAction.OnRemoveTicker -> handleOnRemoveTicker(action.item)
            HomeUiAction.OnSearchClick -> handleOnSearchClick()
            HomeUiAction.OnThemeChangeClick -> handleOnThemeChangeClick()
            is HomeUiAction.OnNewsClick -> handleOnNewsClick(action.ticker, action.url)
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

            delay(REFRESH_DELAY)

            subscribeForUpdates()
        }
    }

    private fun handleOnErrorMessageClose() {
        _uiState.update { state ->
            state.copy(errorUiState = HomeUiState.ErrorUiState.None)
        }
    }

    private fun handleOnNewsClick(ticker: String, url: String?) {
        // Track news click
        analytics.logNewsClicked(ticker, url != null)

        viewModelScope.launch {
            url?.let { _uiEvent.emit(HomeUiEvent.OpenNewsUri(url)) }
        }
    }

    private fun subscribeForUpdates() {
        infoUpdatesJob?.cancel()
        newsUpdatesJob?.cancel()

        infoUpdatesJob = viewModelScope.launch {
            observeTickersInfoUseCase(observeWatchlistItemsUseCase())
                .catch {
                    emit(emptyMap())
                    _uiState.update { state ->
                        state.copy(errorUiState = HomeUiState.ErrorUiState.Error)
                    }
                }
                .collect { info ->
                    _uiState.update { state ->
                        state.copy(info = info)
                    }
                }
        }

        newsUpdatesJob = viewModelScope.launch {
            observeTickersNewsUseCase(observeWatchlistItemsUseCase())
                .catch {
                    emit(emptyList())
                    _uiState.update { state ->
                        state.copy(errorUiState = HomeUiState.ErrorUiState.Error)
                    }
                }
                .collect { news ->
                    _uiState.update { state ->
                        state.copy(newsUiState = HomeUiState.NewsUiState.Content(news))
                    }
                }
        }
    }

    private fun handleOnRefresh() {
        if (_uiState.value.tickers.isEmpty() || _uiState.value.isRefreshing) {
            return
        }

        // Track refresh event
        analytics.logRefreshTriggered()

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isRefreshing = true)
            }

            refreshUseCase()
            subscribeForUpdates()

            delay(REFRESH_DELAY)

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
