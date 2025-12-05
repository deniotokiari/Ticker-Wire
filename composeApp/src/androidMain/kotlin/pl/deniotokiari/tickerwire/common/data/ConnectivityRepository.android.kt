package pl.deniotokiari.tickerwire.common.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.core.annotation.Single

@Single
actual class ConnectivityRepository {
    private val context: Context
        get() = AndroidContextHolder.context

    actual fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

