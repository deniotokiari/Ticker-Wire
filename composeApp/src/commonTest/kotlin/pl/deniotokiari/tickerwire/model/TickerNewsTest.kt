package pl.deniotokiari.tickerwire.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TickerNewsTest {

    private val appleTicker = Ticker(symbol = "AAPL", company = "Apple Inc")
    private val googleTicker = Ticker(symbol = "GOOGL", company = "Alphabet Inc")

    @Test
    fun tickerNewsCreationWithAllFields() {
        val news = TickerNews(
            ticker = appleTicker,
            title = "Apple Reports Record Earnings",
            provider = "TechNews",
            dateTimeFormatted = "2024-01-15 12:00:00",
            timestamp = 1705320000000L,
            url = "https://example.com/news/apple"
        )

        assertEquals(appleTicker, news.ticker)
        assertEquals("Apple Reports Record Earnings", news.title)
        assertEquals("TechNews", news.provider)
        assertEquals("2024-01-15 12:00:00", news.dateTimeFormatted)
        assertEquals(1705320000000L, news.timestamp)
        assertEquals("https://example.com/news/apple", news.url)
    }

    @Test
    fun tickerNewsCreationWithNullUrl() {
        val news = TickerNews(
            ticker = appleTicker,
            title = "Market Update",
            provider = "FinanceNews",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        assertNotNull(news.title)
        assertNull(news.url)
    }

    @Test
    fun tickerNewsCreationWithNullProvider() {
        val news = TickerNews(
            ticker = appleTicker,
            title = "Market Update",
            provider = null,
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        assertNull(news.provider)
    }

    @Test
    fun tickerNewsEquality() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        val news2 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        val news3 = TickerNews(
            ticker = appleTicker,
            title = "Different Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        assertEquals(news1, news2)
        assertNotEquals(news1, news3)
    }

    @Test
    fun tickerNewsCopy() {
        val original = TickerNews(
            ticker = appleTicker,
            title = "Original Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        val copy = original.copy(title = "Modified Title")

        assertEquals("Modified Title", copy.title)
        assertEquals(original.ticker, copy.ticker)
        assertEquals(original.provider, copy.provider)
        assertEquals(original.url, copy.url)
    }

    @Test
    fun tickerNewsHashCodeConsistency() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        val news2 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        assertEquals(news1.hashCode(), news2.hashCode())
    }

    @Test
    fun tickerNewsWithDifferentUrlsAreNotEqual() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com/1"
        )

        val news2 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com/2"
        )

        assertNotEquals(news1, news2)
    }

    @Test
    fun tickerNewsWithNullAndNonNullUrlAreNotEqual() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        val news2 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = "https://example.com"
        )

        assertNotEquals(news1, news2)
    }

    @Test
    fun tickerNewsWithDifferentTickersAreNotEqual() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        val news2 = TickerNews(
            ticker = googleTicker,
            title = "Title",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        assertNotEquals(news1, news2)
    }

    @Test
    fun tickerNewsListOperations() {
        val news1 = TickerNews(
            ticker = appleTicker,
            title = "News 1",
            provider = "Provider",
            dateTimeFormatted = "2024-01-15",
            timestamp = 1705320000000L,
            url = null
        )

        val news2 = TickerNews(
            ticker = appleTicker,
            title = "News 2",
            provider = "Provider",
            dateTimeFormatted = "2024-01-14",
            timestamp = 1705233600000L,
            url = null
        )

        val news3 = TickerNews(
            ticker = appleTicker,
            title = "News 3",
            provider = "Provider",
            dateTimeFormatted = "2024-01-13",
            timestamp = 1705147200000L,
            url = null
        )

        val list = listOf(news1, news2, news3)
        val sorted = list.sortedByDescending { it.timestamp }

        assertEquals(news1, sorted[0])
        assertEquals(news2, sorted[1])
        assertEquals(news3, sorted[2])
    }
}
