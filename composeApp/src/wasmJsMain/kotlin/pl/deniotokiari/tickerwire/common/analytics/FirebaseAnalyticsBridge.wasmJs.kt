@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package pl.deniotokiari.tickerwire.common.analytics

/**
 * WasmJS implementation using Firebase Analytics JS SDK via JS interop.
 * Firebase is initialized in index.html and exposed as window.firebaseAnalytics
 */
actual class FirebaseAnalyticsBridge {

    actual fun logEvent(name: String, params: Map<String, Any>?) {
        try {
            val jsParams = params?.toJsObject() ?: createEmptyJsObject()
            firebaseLogEvent(name, jsParams)
        } catch (e: Exception) {
            println("[WasmJS Analytics] logEvent error: ${e.message}")
        }
    }

    actual fun setUserId(userId: String?) {
        try {
            firebaseSetUserId(userId ?: "")
        } catch (e: Exception) {
            println("[WasmJS Analytics] setUserId error: ${e.message}")
        }
    }

    actual fun setUserProperty(name: String, value: String?) {
        try {
            val props = createEmptyJsObject()
            setJsStringProperty(props, name, value ?: "")
            firebaseSetUserProperties(props)
        } catch (e: Exception) {
            println("[WasmJS Analytics] setUserProperty error: ${e.message}")
        }
    }

    private fun Map<String, Any>.toJsObject(): JsAny {
        val obj = createEmptyJsObject()
        forEach { (key, value) ->
            when (value) {
                is String -> setJsStringProperty(obj, key, value)
                is Int -> setJsIntProperty(obj, key, value)
                is Long -> setJsDoubleProperty(obj, key, value.toDouble())
                is Double -> setJsDoubleProperty(obj, key, value)
                is Boolean -> setJsBooleanProperty(obj, key, value)
                else -> setJsStringProperty(obj, key, value.toString())
            }
        }
        return obj
    }
}

// JS interop functions using @JsFun annotation
@JsFun("() => { return {}; }")
private external fun createEmptyJsObject(): JsAny

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setJsStringProperty(obj: JsAny, key: String, value: String)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setJsIntProperty(obj: JsAny, key: String, value: Int)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setJsDoubleProperty(obj: JsAny, key: String, value: Double)

@JsFun("(obj, key, value) => { obj[key] = value; }")
private external fun setJsBooleanProperty(obj: JsAny, key: String, value: Boolean)

@JsFun("(name, params) => { if (window.firebaseAnalytics) window.firebaseAnalytics.logEvent(name, params); else console.log('[Analytics]', name, params); }")
private external fun firebaseLogEvent(name: String, params: JsAny)

@JsFun("(userId) => { if (window.firebaseAnalytics) window.firebaseAnalytics.setUserId(userId); }")
private external fun firebaseSetUserId(userId: String)

@JsFun("(props) => { if (window.firebaseAnalytics) window.firebaseAnalytics.setUserProperties(props); }")
private external fun firebaseSetUserProperties(props: JsAny)
