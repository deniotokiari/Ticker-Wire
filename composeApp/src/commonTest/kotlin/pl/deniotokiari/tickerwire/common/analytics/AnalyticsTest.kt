package pl.deniotokiari.tickerwire.common.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnalyticsTest {

    // Fake FirebaseAnalyticsBridge for testing
    private class FakeFirebaseAnalyticsBridge {
        val loggedEvents = mutableListOf<Pair<String, Map<String, Any>?>>()
        private var _userId: String? = null
        val currentUserId: String? get() = _userId
        val userProperties = mutableMapOf<String, String?>()

        fun logEvent(name: String, params: Map<String, Any>?) {
            loggedEvents.add(name to params)
        }

        fun setUserId(userId: String?) {
            _userId = userId
        }

        fun setUserProperty(name: String, value: String?) {
            userProperties[name] = value
        }
    }

    // Testable Analytics wrapper
    private class TestableAnalytics(
        private val bridge: FakeFirebaseAnalyticsBridge
    ) {
        fun logEvent(name: String, params: Map<String, Any>? = null) {
            bridge.logEvent(name, params)
        }

        fun logScreenView(screenName: String) {
            bridge.logEvent("screen_view", mapOf(
                "screen_name" to screenName,
                "screen_class" to screenName
            ))
        }

        fun setUserId(userId: String?) {
            bridge.setUserId(userId)
        }

        fun setUserProperty(name: String, value: String?) {
            bridge.setUserProperty(name, value)
        }

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

    @Test
    fun logEventLogsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logEvent("test_event", mapOf("key" to "value"))

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals("test_event", bridge.loggedEvents[0].first)
        assertEquals(mapOf("key" to "value"), bridge.loggedEvents[0].second)
    }

    @Test
    fun logEventWithoutParamsLogsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logEvent("simple_event")

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals("simple_event", bridge.loggedEvents[0].first)
        assertNull(bridge.loggedEvents[0].second)
    }

    @Test
    fun logScreenViewLogsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logScreenView("home")

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals("screen_view", bridge.loggedEvents[0].first)
        assertEquals("home", bridge.loggedEvents[0].second?.get("screen_name"))
        assertEquals("home", bridge.loggedEvents[0].second?.get("screen_class"))
    }

    @Test
    fun setUserIdSetsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.setUserId("user123")

        assertEquals("user123", bridge.currentUserId)
    }

    @Test
    fun setUserIdCanBeNull() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.setUserId("user123")
        analytics.setUserId(null)

        assertNull(bridge.currentUserId)
    }

    @Test
    fun setUserPropertySetsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.setUserProperty("premium", "true")

        assertEquals("true", bridge.userProperties["premium"])
    }

    @Test
    fun logTickerAddedLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logTickerAdded("AAPL")

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.TICKER_ADDED, bridge.loggedEvents[0].first)
        assertEquals("AAPL", bridge.loggedEvents[0].second?.get(AnalyticsParams.TICKER_SYMBOL))
    }

    @Test
    fun logTickerRemovedLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logTickerRemoved("GOOGL")

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.TICKER_REMOVED, bridge.loggedEvents[0].first)
        assertEquals("GOOGL", bridge.loggedEvents[0].second?.get(AnalyticsParams.TICKER_SYMBOL))
    }

    @Test
    fun logSearchPerformedLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logSearchPerformed(5)

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.SEARCH_PERFORMED, bridge.loggedEvents[0].first)
        assertEquals(5, bridge.loggedEvents[0].second?.get(AnalyticsParams.QUERY_LENGTH))
    }

    @Test
    fun logRefreshTriggeredLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logRefreshTriggered()

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.REFRESH_TRIGGERED, bridge.loggedEvents[0].first)
    }

    @Test
    fun logThemeChangedLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logThemeChanged("dark")

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.THEME_CHANGED, bridge.loggedEvents[0].first)
        assertEquals("dark", bridge.loggedEvents[0].second?.get(AnalyticsParams.THEME))
    }

    @Test
    fun logNewsClickedLogsCorrectEvent() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logNewsClicked("TSLA", hasUrl = true)

        assertEquals(1, bridge.loggedEvents.size)
        assertEquals(AnalyticsEvents.NEWS_CLICKED, bridge.loggedEvents[0].first)
        assertEquals("TSLA", bridge.loggedEvents[0].second?.get(AnalyticsParams.TICKER_SYMBOL))
        assertEquals(true, bridge.loggedEvents[0].second?.get(AnalyticsParams.HAS_URL))
    }

    @Test
    fun logNewsClickedWithoutUrlLogsCorrectly() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logNewsClicked("MSFT", hasUrl = false)

        assertEquals(false, bridge.loggedEvents[0].second?.get(AnalyticsParams.HAS_URL))
    }

    @Test
    fun multipleEventsAreLoggedInOrder() {
        val bridge = FakeFirebaseAnalyticsBridge()
        val analytics = TestableAnalytics(bridge)

        analytics.logScreenView("home")
        analytics.logTickerAdded("AAPL")
        analytics.logRefreshTriggered()

        assertEquals(3, bridge.loggedEvents.size)
        assertEquals("screen_view", bridge.loggedEvents[0].first)
        assertEquals(AnalyticsEvents.TICKER_ADDED, bridge.loggedEvents[1].first)
        assertEquals(AnalyticsEvents.REFRESH_TRIGGERED, bridge.loggedEvents[2].first)
    }
}
