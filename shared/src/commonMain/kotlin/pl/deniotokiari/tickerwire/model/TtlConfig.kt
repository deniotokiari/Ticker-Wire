package pl.deniotokiari.tickerwire.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TtlConfig(
    @SerialName("search_ttl_ms")
    val searchTtlMs: Long,
    @SerialName("news_ttl_ms")
    val newsTtlMs: Long,
    @SerialName("info_ttl_ms")
    val infoTtlMs: Long,
)

@Serializable
data class AppTtlConfig(
    val server: TtlConfig,
    val client: TtlConfig,
)
