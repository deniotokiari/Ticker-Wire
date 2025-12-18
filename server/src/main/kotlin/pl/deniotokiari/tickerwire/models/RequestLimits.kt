package pl.deniotokiari.tickerwire.models

import kotlinx.serialization.Serializable

@Serializable
data class RequestLimits(
    val news: Int,
    val tickers: Int,
)
