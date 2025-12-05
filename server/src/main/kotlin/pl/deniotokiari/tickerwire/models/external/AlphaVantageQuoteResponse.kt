package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Alpha Vantage Global Quote API Response
 * Reference: https://www.alphavantage.co/documentation/#latestprice
 */
@Serializable
data class AlphaVantageQuoteResponse(
    @SerialName("Global Quote") val globalQuote: AlphaVantageQuote? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Error Message") val errorMessage: String? = null,
)

@Serializable
data class AlphaVantageQuote(
    @SerialName("05. price") val price: String,
    @SerialName("09. change") val change: String,
    @SerialName("10. change percent") val changePercent: String,
)

