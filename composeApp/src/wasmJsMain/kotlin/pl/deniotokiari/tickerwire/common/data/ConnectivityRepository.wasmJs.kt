package pl.deniotokiari.tickerwire.common.data

import kotlinx.browser.window
import org.koin.core.annotation.Single

@Single
actual class ConnectivityRepository {
    private var isConnected: Boolean = window.navigator.onLine

    init {
        window.addEventListener("online", { isConnected = true })
        window.addEventListener("offline", { isConnected = false })
    }

    actual fun isOnline(): Boolean = isConnected
}

