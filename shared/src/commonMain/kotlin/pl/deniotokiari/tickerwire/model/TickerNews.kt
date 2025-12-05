package pl.deniotokiari.tickerwire.model

import kotlinx.serialization.Serializable

@Serializable
data class TickerNews(
    val ticker: Ticker,
    val title: String,
    val provider: String?,
    val dateTimeFormatted: String,
    val timestamp: Long,
    val url: String?,
)
