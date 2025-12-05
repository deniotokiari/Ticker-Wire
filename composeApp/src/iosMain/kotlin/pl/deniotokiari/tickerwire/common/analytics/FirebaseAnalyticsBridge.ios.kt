package pl.deniotokiari.tickerwire.common.analytics

/**
 * iOS implementation for Firebase Analytics using callback bridge.
 * Swift sets the callbacks at app startup, Kotlin calls them.
 */
actual class FirebaseAnalyticsBridge {
    
    actual fun logEvent(name: String, params: Map<String, Any>?) {
        IosAnalyticsBridge.logEvent?.invoke(name, params)
    }

    actual fun setUserId(userId: String?) {
        IosAnalyticsBridge.setUserId?.invoke(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        IosAnalyticsBridge.setUserProperty?.invoke(name, value)
    }
}

/**
 * Bridge object - Swift sets these callbacks at app startup
 */
object IosAnalyticsBridge {
    var logEvent: ((String, Map<String, Any>?) -> Unit)? = null
    var setUserId: ((String?) -> Unit)? = null
    var setUserProperty: ((String, String?) -> Unit)? = null
}
