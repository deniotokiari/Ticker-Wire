package pl.deniotokiari.tickerwire.feature.search.presentation

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import pl.deniotokiari.tickerwire.common.analytics.Analytics
import pl.deniotokiari.tickerwire.feature.search.domain.SearchTickersUseCase

private const val SEARCH_DELAY = 400L

@KoinViewModel
class SearchViewModel(
    private val analytics: Analytics,
    private val searchTickersUseCase: SearchTickersUseCase,
) : ViewModel() {
    private var activeJob: Job? = null

    private val _uiEvent = MutableSharedFlow<SearchUiEvent>()
    val uiEvent: SharedFlow<SearchUiEvent> = _uiEvent.asSharedFlow()

    private val _uiState: MutableStateFlow<SearchUiState> = MutableStateFlow(SearchUiState.Idle())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Track screen view
        analytics.logScreenView("search")
    }

    fun onAction(action: SearchUiAction) {
        when (action) {
            SearchUiAction.OnBackClicked -> handleOnBackClicked()
            SearchUiAction.OnErrorMessageActionClicked -> handleOnErrorMessageActionClicked()
            SearchUiAction.OnErrorMessageClosed -> handleOnErrorMessageClosed()
            is SearchUiAction.OnQueryChanged -> handleOnQueryChanged(action.query)
            is SearchUiAction.OnSearchItemClicked -> handleOnSearchItemClicked(action)
        }
    }

    private fun handleOnBackClicked() {
        viewModelScope.launch {
            _uiEvent.emit(SearchUiEvent.NavigateBack)
        }
    }

    private fun handleOnErrorMessageActionClicked() {
        handleOnQueryChanged(_uiState.value.query)
    }

    private fun handleOnErrorMessageClosed() {
        _uiState.update { state ->
            SearchUiState.Idle(state.query)
        }
    }

    private fun handleOnQueryChanged(query: String) {
        activeJob?.cancel()

        if (query.isEmpty()) {
            _uiState.update { SearchUiState.Idle() }
        } else {
            _uiState.update { SearchUiState.Loading(query) }

            activeJob = viewModelScope.launch {
                delay(SEARCH_DELAY)

                // Track search performed
                analytics.logSearchPerformed(query.length)

                searchTickersUseCase(query).fold(
                    onSuccess = { result ->
                        if (result.isEmpty()) {
                            _uiState.update { SearchUiState.Empty(query = query) }
                        } else {
                            _uiState.update {
                                SearchUiState.Content(
                                    query = query,
                                    items = result,
                                )
                            }
                        }
                    },
                    onFailure = {
                        println("onFailure => $it")
                        _uiState.update { SearchUiState.Error(query) }
                    },
                )
            }
        }
    }

    private fun handleOnSearchItemClicked(action: SearchUiAction.OnSearchItemClicked) {
        viewModelScope.launch {
            _uiEvent.emit(SearchUiEvent.NavigateBackWithSelectedSearchItem(action.item))
        }
    }
}
