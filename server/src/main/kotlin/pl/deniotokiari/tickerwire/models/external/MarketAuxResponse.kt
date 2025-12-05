package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Marketaux API Response DTOs
 * Documentation: https://www.marketaux.com/documentation
 */

// ============ Entity Search Response ============
// GET https://api.marketaux.com/v1/entity/search

@Serializable
data class MarketAuxEntitySearchResponse(
    val data: List<MarketAuxEntity>? = null,
    val error: MarketAuxError? = null,
)

@Serializable
data class MarketAuxEntity(
    val symbol: String? = null,
    val name: String? = null,
    val type: String? = null,
    val industry: String? = null,
    val country: String? = null,
)

// ============ News Response ============
// GET https://api.marketaux.com/v1/news/all

@Serializable
data class MarketAuxNewsResponse(
    val data: List<MarketAuxNewsItem>? = null,
    val error: MarketAuxError? = null,
)

@Serializable
data class MarketAuxNewsItem(
    val uuid: String? = null,
    val title: String? = null,
    val source: String? = null,
    val url: String? = null,
    @SerialName("published_at")
    val publishedAt: String? = null,
    val entities: List<MarketAuxNewsEntity>? = null,
)

@Serializable
data class MarketAuxNewsEntity(
    val symbol: String? = null,
    val name: String? = null,
    @SerialName("sentiment_score")
    val sentimentScore: Double? = null,
)

// ============ Common Error ============

@Serializable
data class MarketAuxError(
    val code: String? = null,
    val message: String? = null,
)

