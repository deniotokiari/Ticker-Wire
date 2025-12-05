package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Massive.com API Response DTOs
 * Documentation: https://massive.com/docs/rest/stocks/overview
 *
 * Free tier: 5 API calls per minute
 *
 * Note: Massive API is similar to Polygon.io API
 */

// ============ Tickers List Response ============
// GET /v3/reference/tickers?search={query}

@Serializable
data class MassiveTickersResponse(
    val status: String? = null,
    val results: List<MassiveTicker>? = null,
    val count: Int? = null,
    @SerialName("next_url")
    val nextUrl: String? = null,
)

@Serializable
data class MassiveTicker(
    val ticker: String? = null,
    val name: String? = null,
    val market: String? = null,
    val locale: String? = null,
    @SerialName("primary_exchange")
    val primaryExchange: String? = null,
    val type: String? = null,
    val active: Boolean? = null,
    @SerialName("currency_name")
    val currencyName: String? = null,
)

// ============ Snapshot Response ============
// GET /v2/snapshot/locale/us/markets/stocks/tickers/{ticker}

@Serializable
data class MassiveSnapshotResponse(
    val status: String? = null,
    val ticker: MassiveSnapshotTicker? = null,
)

@Serializable
data class MassiveSnapshotTicker(
    val ticker: String? = null,
    val day: MassiveAggregateBar? = null,
    val prevDay: MassiveAggregateBar? = null,
    @SerialName("todaysChange")
    val todaysChange: Double? = null,
    @SerialName("todaysChangePerc")
    val todaysChangePerc: Double? = null,
    val updated: Long? = null,
)

@Serializable
data class MassiveAggregateBar(
    val c: Double? = null,  // Close price
    val h: Double? = null,  // High price
    val l: Double? = null,  // Low price
    val o: Double? = null,  // Open price
    val v: Double? = null,  // Volume
    val vw: Double? = null, // Volume weighted average price
)

// ============ Previous Close Response ============
// GET /v2/aggs/ticker/{ticker}/prev

@Serializable
data class MassivePrevCloseResponse(
    val status: String? = null,
    val ticker: String? = null,
    val results: List<MassivePrevBar>? = null,
    val resultsCount: Int? = null,
)

@Serializable
data class MassivePrevBar(
    val c: Double? = null,  // Close price
    val h: Double? = null,  // High price
    val l: Double? = null,  // Low price
    val o: Double? = null,  // Open price
    val v: Double? = null,  // Volume
    val vw: Double? = null, // Volume weighted average price
    val t: Long? = null,    // Timestamp
)

// ============ News Response ============
// GET /v3/reference/news?ticker={ticker}

@Serializable
data class MassiveNewsResponse(
    val status: String? = null,
    val results: List<MassiveNewsItem>? = null,
    val count: Int? = null,
)

@Serializable
data class MassiveNewsItem(
    val id: String? = null,
    val publisher: MassivePublisher? = null,
    val title: String? = null,
    val author: String? = null,
    @SerialName("published_utc")
    val publishedUtc: String? = null,
    @SerialName("article_url")
    val articleUrl: String? = null,
    val tickers: List<String>? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val description: String? = null,
)

@Serializable
data class MassivePublisher(
    val name: String? = null,
    @SerialName("homepage_url")
    val homepageUrl: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
)

