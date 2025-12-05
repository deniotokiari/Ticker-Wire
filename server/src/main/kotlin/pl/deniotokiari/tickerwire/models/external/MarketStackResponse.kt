package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.Serializable

/**
 * MarketStack API v2 Response DTOs
 * Documentation: https://docs.apilayer.com/marketstack/docs/marketstack-api-v2-v-2-0-0
 */

// ============ Common ============

@Serializable
data class MarketStackError(
    val code: String? = null,
    val message: String? = null,
)

// ============ Tickers List Response ============
// GET /tickerslist

@Serializable
data class MarketStackTickersListResponse(
    val data: List<MarketStackTickerListItem>? = null,
    val error: MarketStackError? = null,
)

@Serializable
data class MarketStackTickerListItem(
    val ticker: String? = null,
    val name: String? = null,
)

// ============ EOD Response (for info) ============
// GET /eod/latest

@Serializable
data class MarketStackEodResponse(
    val data: List<MarketStackEodBar>? = null,
    val error: MarketStackError? = null,
)

@Serializable
data class MarketStackEodBar(
    val open: Double? = null,
    val close: Double? = null,
    val symbol: String? = null,
)

