package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Financial Modeling Prep API Response DTOs
 * Documentation: https://site.financialmodelingprep.com/developer/docs
 *
 * Note: News endpoint requires paid plan
 */

// ============ Search Response ============
// GET /stable/search-symbol?query=AAPL

@Serializable
data class FmpSearchResult(
    val symbol: String? = null,
    val name: String? = null,
    val currency: String? = null,
    val stockExchange: String? = null,
    val exchangeShortName: String? = null,
)

// ============ Quote Response ============
// GET /stable/quote?symbol=AAPL

@Serializable
data class FmpQuote(
    val symbol: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val change: Double? = null,
    val changesPercentage: Double? = null,
    val dayLow: Double? = null,
    val dayHigh: Double? = null,
    val yearHigh: Double? = null,
    val yearLow: Double? = null,
    val marketCap: Long? = null,
    val priceAvg50: Double? = null,
    val priceAvg200: Double? = null,
    val exchange: String? = null,
    val volume: Long? = null,
    val avgVolume: Long? = null,
    val open: Double? = null,
    val previousClose: Double? = null,
    val eps: Double? = null,
    val pe: Double? = null,
    val earningsAnnouncement: String? = null,
    val sharesOutstanding: Long? = null,
    val timestamp: Long? = null,
)

// ============ Error Response ============

@Serializable
data class FmpErrorResponse(
    @SerialName("Error Message")
    val errorMessage: String? = null,
)
