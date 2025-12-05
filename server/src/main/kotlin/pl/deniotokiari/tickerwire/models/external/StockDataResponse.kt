package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * StockData.org API Response DTOs
 * Documentation: https://www.stockdata.org/documentation
 */

// ============ Stock Search Response ============
// GET https://api.stockdata.org/v1/data/search

@Serializable
data class StockDataSearchResponse(
    val data: List<StockDataSearchItem>? = null,
    val error: StockDataError? = null,
)

@Serializable
data class StockDataSearchItem(
    val ticker: String? = null,
    val name: String? = null,
)

// ============ Stock Quote Response ============
// GET https://api.stockdata.org/v1/data/quote

@Serializable
data class StockDataQuoteResponse(
    val data: List<StockDataQuote>? = null,
    val error: StockDataError? = null,
)

@Serializable
data class StockDataQuote(
    val ticker: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    @SerialName("day_change")
    val dayChange: Double? = null,
    @SerialName("previous_close_price")
    val previousClosePrice: Double? = null,
)

// ============ News Response ============
// GET https://api.stockdata.org/v1/news/all

@Serializable
data class StockDataNewsResponse(
    val data: List<StockDataNewsItem>? = null,
    val error: StockDataError? = null,
)

@Serializable
data class StockDataNewsItem(
    val uuid: String? = null,
    val title: String? = null,
    val source: String? = null,
    val url: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val entities: List<StockDataEntity>? = null,
)

@Serializable
data class StockDataEntity(
    val symbol: String? = null,
    val name: String? = null,
)

// ============ Common Error ============

@Serializable
data class StockDataError(
    val code: String? = null,
    val message: String? = null,
)

