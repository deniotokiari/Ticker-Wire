package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.Serializable

/**
 * Finnhub API Response DTOs
 * Documentation: https://finnhub.io/docs/api
 *
 * Free tier: 60 API calls per minute
 */

// ============ Symbol Search Response ============
// GET /api/v1/search?q={query}

@Serializable
data class FinnhubSearchResponse(
    val count: Int? = null,
    val result: List<FinnhubSearchResult>? = null,
)

@Serializable
data class FinnhubSearchResult(
    val description: String? = null,
    val displaySymbol: String? = null,
    val symbol: String? = null,
    val type: String? = null,
)

// ============ Quote Response ============
// GET /api/v1/quote?symbol={symbol}

@Serializable
data class FinnhubQuote(
    val c: Double? = null,   // Current price
    val d: Double? = null,   // Change
    val dp: Double? = null,  // Percent change
    val h: Double? = null,   // High price of the day
    val l: Double? = null,   // Low price of the day
    val o: Double? = null,   // Open price of the day
    val pc: Double? = null,  // Previous close price
    val t: Long? = null,     // Timestamp
)

// ============ Company News Response ============
// GET /api/v1/company-news?symbol={symbol}&from={date}&to={date}

@Serializable
data class FinnhubNewsItem(
    val category: String? = null,
    val datetime: Long? = null,       // Unix timestamp
    val headline: String? = null,
    val id: Long? = null,
    val image: String? = null,
    val related: String? = null,
    val source: String? = null,
    val summary: String? = null,
    val url: String? = null,
)

