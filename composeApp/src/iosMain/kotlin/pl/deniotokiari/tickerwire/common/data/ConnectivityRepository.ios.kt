package pl.deniotokiari.tickerwire.common.data

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Single
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation using NWPathMonitor (Network framework)
 * This is the modern, recommended approach for iOS 12+
 */
@Single
@OptIn(ExperimentalForeignApi::class)
actual class ConnectivityRepository {
    private var isConnected: Boolean = true

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_set_update_handler(monitor) { path ->
            isConnected = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_start(monitor)
    }

    actual fun isOnline(): Boolean = isConnected
}
