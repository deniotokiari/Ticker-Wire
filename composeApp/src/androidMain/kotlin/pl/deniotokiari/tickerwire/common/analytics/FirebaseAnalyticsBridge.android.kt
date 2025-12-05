package pl.deniotokiari.tickerwire.common.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Android implementation using native Firebase Analytics SDK.
 * Requires google-services.json in composeApp/ directory.
 */
actual class FirebaseAnalyticsBridge {
    private val analytics: FirebaseAnalytics = Firebase.analytics

    actual fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = params?.toBundle()
        analytics.logEvent(name, bundle)
    }

    actual fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}

