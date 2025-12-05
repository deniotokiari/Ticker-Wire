package pl.deniotokiari.tickerwire.model

import kotlinx.serialization.Serializable

@Serializable
data class TickerData(
    val ticker: Ticker,
    val marketValueFormatted: String,
    val deltaFormatted: String,
    val percentFormatted: String,
    val currency: String,
)
