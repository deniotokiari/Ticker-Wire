package pl.deniotokiari.tickerwire.models.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Alpha Vantage Symbol Search API Response
 * Reference: https://www.alphavantage.co/documentation/#symbolsearch
 */
@Serializable
data class AlphaVantageSearchResponse(
    @SerialName("bestMatches") val bestMatches: List<AlphaVantageSearchMatch>? = null,
    @SerialName("Note") val note: String? = null,
    @SerialName("Error Message") val errorMessage: String? = null,
)

@Serializable
data class AlphaVantageSearchMatch(
    @SerialName("1. symbol") val symbol: String,
    @SerialName("2. name") val name: String,
)

