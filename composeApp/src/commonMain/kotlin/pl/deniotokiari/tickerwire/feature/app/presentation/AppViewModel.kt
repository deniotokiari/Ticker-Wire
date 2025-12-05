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

@KoinViewModel
class AppViewModel(
    private val observeIsDarkThemeUseCase: ObserveIsDarkThemeUseCase,
    isDarkThemeUseCase: IsDarkThemeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState(isDarkTheme = isDarkThemeUseCase()))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeIsDarkThemeUseCase().collect { isDarkTheme ->
                _uiState.update { state ->
                    state.copy(isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}
