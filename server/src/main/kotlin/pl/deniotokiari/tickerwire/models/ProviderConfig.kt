package pl.deniotokiari.tickerwire.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Provider configuration - stored in Firebase Remote Config
 * Contains API credentials and rate limit configuration
 */
@Serializable
data class ProviderConfig(
    @SerialName("api_uri")
    val apiUri: String,
    @SerialName("api_key")
    val apiKey: String,
    @SerialName("limit")
    val limit: LimitConfig,
)
