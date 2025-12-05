package pl.deniotokiari.tickerwire.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TickerNewsDto(
    val title: String,
    val provider: String?,
    val dateTimeFormatted: String,
    val timestamp: Long,
    val url: String?,
)
