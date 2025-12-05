package pl.deniotokiari.tickerwire.common.platform

import androidx.compose.runtime.Composable

/**
 * Sets the status bar appearance based on the theme.
 * @param isDarkTheme true for dark theme (light status bar icons), false for light theme (dark status bar icons)
 */
@Composable
expect fun SetStatusBarAppearance(isDarkTheme: Boolean)

