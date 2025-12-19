package pl.deniotokiari.tickerwire.common.analytics

import org.koin.core.annotation.Single

/**
 * Firebase Analytics - KMP layer.
 * All analytics logic here, platform code just passes data to native Firebase SDKs.
 */
@Single
class Analytics(
    private val firebaseAnalytics: FirebaseAnalyticsBridge
) {
    fun logEvent(name: String, params: Map<String, Any>? = null) {
        firebaseAnalytics.logEvent(name, params)
    }

    fun logScreenView(screenName: String) {
        firebaseAnalytics.logEvent("screen_view", mapOf(
            "screen_name" to screenName,
            "screen_class" to screenName
        ))
    }

    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    // Convenience methods with predefined events
    fun logTickerAdded(symbol: String) {
        logEvent(AnalyticsEvents.TICKER_ADDED, mapOf(AnalyticsParams.TICKER_SYMBOL to symbol))
    }

    fun logTickerRemoved(symbol: String) {
        logEvent(AnalyticsEvents.TICKER_REMOVED, mapOf(AnalyticsParams.TICKER_SYMBOL to symbol))
    }

    fun logSearchPerformed(queryLength: Int) {
        logEvent(AnalyticsEvents.SEARCH_PERFORMED, mapOf(AnalyticsParams.QUERY_LENGTH to queryLength))
    }

    fun logRefreshTriggered() {
        logEvent(AnalyticsEvents.REFRESH_TRIGGERED)
    }

    fun logThemeChanged(theme: String) {
        logEvent(AnalyticsEvents.THEME_CHANGED, mapOf(AnalyticsParams.THEME to theme))
    }

    fun logNewsClicked(ticker: String, hasUrl: Boolean) {
        logEvent(AnalyticsEvents.NEWS_CLICKED, mapOf(
            AnalyticsParams.TICKER_SYMBOL to ticker,
            AnalyticsParams.HAS_URL to hasUrl
        ))
    }
}

@Single
fun provideFirebaseAnalyticsBridge(): FirebaseAnalyticsBridge = FirebaseAnalyticsBridge()

/**
 * Platform bridge interface - implemented by each platform with native Firebase SDK
 */
expect class FirebaseAnalyticsBridge() {
    fun logEvent(name: String, params: Map<String, Any>?)
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
}

/** Analytics event names */
object AnalyticsEvents {
    const val SCREEN_HOME = "home_screen"
    const val SCREEN_SEARCH = "search_screen"
    const val TICKER_ADDED = "ticker_added"
    const val TICKER_REMOVED = "ticker_removed"
    const val SEARCH_PERFORMED = "search_performed"
    const val REFRESH_TRIGGERED = "refresh_triggered"
    const val THEME_CHANGED = "theme_changed"
    const val NEWS_CLICKED = "news_clicked"
}

/** Analytics parameter names */
object AnalyticsParams {
    const val TICKER_SYMBOL = "ticker_symbol"
    const val QUERY_LENGTH = "query_length"
    const val THEME = "theme"
    const val HAS_URL = "has_url"
}
