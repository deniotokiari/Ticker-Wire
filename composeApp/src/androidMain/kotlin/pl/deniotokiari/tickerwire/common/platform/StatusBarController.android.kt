package pl.deniotokiari.tickerwire.common.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetStatusBarAppearance(isDarkTheme: Boolean) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window ?: return
        
        SideEffect {
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Dark theme = light icons, Light theme = dark icons
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
        }
    }
}

