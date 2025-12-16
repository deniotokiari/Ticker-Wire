package pl.deniotokiari.tickerwire.feature.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import pl.deniotokiari.tickerwire.common.domain.IsDarkThemeUseCase
import pl.deniotokiari.tickerwire.common.domain.ObserveIsDarkThemeUseCase
import pl.deniotokiari.tickerwire.feature.app.domain.RefreshTtlConfigUseCase
import pl.deniotokiari.tickerwire.navigation.Route

@KoinViewModel
class AppViewModel(
    private val observeIsDarkThemeUseCase: ObserveIsDarkThemeUseCase,
    isDarkThemeUseCase: IsDarkThemeUseCase,
    refreshTtlConfigUseCase: RefreshTtlConfigUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            isDarkTheme = isDarkThemeUseCase(),
            startDestination = null,
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeIsDarkThemeUseCase().collect { isDarkTheme ->
                _uiState.update { state ->
                    state.copy(isDarkTheme = isDarkTheme)
                }
            }
        }

        viewModelScope.launch {
            refreshTtlConfigUseCase()

            _uiState.update { state ->
                state.copy(startDestination = Route.Home)
            }
        }
    }
}
