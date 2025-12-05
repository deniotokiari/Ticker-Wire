package pl.deniotokiari.tickerwire.common.analytics

import kotlinx.browser.window

/**
 * Web/JS implementation using Firebase Analytics JS SDK.
 * Requires Firebase to be initialized in index.html.
 */
actual class FirebaseAnalyticsBridge {
    
    actual fun logEvent(name: String, params: Map<String, Any>?) {
        try {
            val analytics = window.asDynamic().firebaseAnalytics
            if (analytics != null) {
                val jsParams = params?.toJsObject() ?: js("{}")
                analytics.logEvent(name, jsParams)
            }
        } catch (e: Exception) {
            console.log("[Analytics] logEvent error: ${e.message}")
        }
    }

    actual fun setUserId(userId: String?) {
        try {
            val analytics = window.asDynamic().firebaseAnalytics
            analytics?.setUserId(userId)
        } catch (e: Exception) {
            console.log("[Analytics] setUserId error: ${e.message}")
        }
    }

    actual fun setUserProperty(name: String, value: String?) {
        try {
            val analytics = window.asDynamic().firebaseAnalytics
            if (analytics != null) {
                val props = js("{}")
                props[name] = value
                analytics.setUserProperties(props)
            }
        } catch (e: Exception) {
            console.log("[Analytics] setUserProperty error: ${e.message}")
        }
    }

    private fun Map<String, Any>.toJsObject(): dynamic {
        val obj = js("{}")
        forEach { (key, value) -> obj[key] = value }
        return obj
    }
}

