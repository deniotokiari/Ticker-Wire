package pl.deniotokiari.tickerwire.model

import kotlinx.serialization.Serializable

@Serializable
data class Ticker(
    val symbol: String,
    val company: String,
)
