package pl.deniotokiari.tickerwire.common.data

import org.koin.core.annotation.Single

/**
 * Repository to check network connectivity status.
 * Uses platform-specific implementations via expect/actual pattern.
 */
@Single
expect class ConnectivityRepository() {
    /**
     * Check if the device currently has an active internet connection.
     * @return true if online, false if offline
     */
    fun isOnline(): Boolean
}

