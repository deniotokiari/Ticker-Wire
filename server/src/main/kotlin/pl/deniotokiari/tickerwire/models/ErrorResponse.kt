package pl.deniotokiari.tickerwire.models

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String,
    val error: String,
    val requestId: String? = null,
)

