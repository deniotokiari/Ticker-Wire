package pl.deniotokiari.tickerwire.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Limit configuration - immutable, stored in Firebase Remote Config
 * Defines the rate limits for a provider
 */
@Serializable
data class LimitConfig(
    @SerialName("per_month")
    val perMonth: Int? = null,
    @SerialName("per_day")
    val perDay: Int? = null,
    @SerialName("per_minute")
    val perMinute: Int? = null,
)

