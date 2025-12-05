package pl.deniotokiari.tickerwire.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TickerDto(
    val ticker: String,
    val company: String,
)
