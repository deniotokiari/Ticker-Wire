package pl.deniotokiari.tickerwire.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.JsonNull.content
import pl.deniotokiari.tickerwire.common.platform.SetStatusBarAppearance

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    errorContainer = LightErrorContainer,
    onError = LightOnError,
    error = LightError,
)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    errorContainer = DarkErrorContainer,
    onError = DarkOnError,
    error = DarkError,
)

@Suppress("ModifierRequired", "MultipleContentEmitters")
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = appTypography()

    // Set status bar appearance based on theme - changes automatically when darkTheme changes
    SetStatusBarAppearance(isDarkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                content()
            }
        },
    )
}

