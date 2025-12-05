package pl.deniotokiari.tickerwire.common.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
actual fun SetStatusBarAppearance(isDarkTheme: Boolean) {
    SideEffect {
        // Update shared state - the view controller will observe this and update the status bar
        StatusBarState.updateTheme(isDarkTheme)
    }
}

