package pl.deniotokiari.tickerwire.common.data

import org.koin.core.annotation.Single
import java.net.InetSocketAddress
import java.net.Socket

@Single
actual class ConnectivityRepository {
    
    actual fun isOnline(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

