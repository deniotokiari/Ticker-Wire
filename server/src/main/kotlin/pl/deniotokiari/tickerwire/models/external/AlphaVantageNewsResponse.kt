package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Alpha Vantage News & Sentiment API Response
 * Reference: https://www.alphavantage.co/documentation/#news-sentiment
 */
@Serializable
data class AlphaVantageNewsResponse(
    @SerialName("feed") val feed: List<AlphaVantageNewsItem>? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Error Message") val errorMessage: String? = null,
)

@Serializable
data class AlphaVantageNewsItem(
    @SerialName("title") val title: String,
    @SerialName("time_published") val timePublished: String,
    @SerialName("source") val source: String,
    @SerialName("url") val url: String? = null,
    @SerialName("ticker_sentiment") val tickerSentiment: List<AlphaVantageTickerSentiment>? = null,
)

@Serializable
data class AlphaVantageTickerSentiment(
    @SerialName("ticker") val ticker: String,
)

