package pl.deniotokiari.tickerwire.common.platform

import platform.Foundation.NSNotificationCenter

/**
 * Shared state for iOS status bar appearance
 * This allows the view controller to observe and update the status bar style
 */
object StatusBarState {
    var isDarkTheme: Boolean = false
        private set
    
    private const val STATUS_BAR_STATE_CHANGED = "StatusBarStateChanged"

    fun updateTheme(isDark: Boolean) {
        if (isDarkTheme != isDark) {
            isDarkTheme = isDark
            // Notify observers that status bar state changed
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = STATUS_BAR_STATE_CHANGED,
                `object` = null
            )
        }
    }
    
    fun getNotificationName(): String = STATUS_BAR_STATE_CHANGED
}

