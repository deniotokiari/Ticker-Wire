package pl.deniotokiari.tickerwire.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TickerInfoDto(
    val marketValueFormatted: String,
    val deltaFormatted: String,
    val percentFormatted: String,
    val currency: String,
)
