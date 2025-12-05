package pl.deniotokiari.tickerwire.common.platform

import androidx.compose.runtime.Composable

@Composable
actual fun SetStatusBarAppearance(isDarkTheme: Boolean) {
    // No-op for WasmJS (web)
}

