package pl.deniotokiari.tickerwire.feature.app.presentation

import pl.deniotokiari.tickerwire.navigation.Route

data class AppUiState(
    val isDarkTheme: Boolean,
    val startDestination: Route?,
)
