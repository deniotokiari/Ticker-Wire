package pl.deniotokiari.tickerwire.adapter.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pl.deniotokiari.tickerwire.adapter.StockNewsProvider
import pl.deniotokiari.tickerwire.adapter.StockSearchProvider
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.external.MarketAuxEntitySearchResponse
import pl.deniotokiari.tickerwire.models.external.MarketAuxNewsItem
import pl.deniotokiari.tickerwire.models.external.MarketAuxNewsResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Marketaux API implementation
 * Documentation: https://www.marketaux.com/documentation
 *
 * Provides:
 * - Entity Search: GET /v1/entity/search
 * - News: GET /v1/news/all
 */
class MarketAuxStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockNewsProvider, StockSearchProvider {

    /**
     * Search for entities (stocks, indices, etc.)
     * GET https://api.marketaux.com/v1/entity/search?search={query}
     */
    override suspend fun search(query: String): List<TickerDto> {
        val (uri, apiKey) = providerConfigService.get(Provider.MARKETAUX)

        val response: MarketAuxEntitySearchResponse = client.get("$uri/entity/search") {
            parameter("api_token", apiKey)
            parameter("search", query)
        }.body()

        if (response.error != null) {
            throw Exception("Marketaux API error: ${response.error.message}")
        }

        return response.data?.mapNotNull { entity ->
            if (entity.symbol != null && entity.name != null) {
                TickerDto(
                    ticker = entity.symbol,
                    company = entity.name,
                )
            } else {
                null
            }
        } ?: emptyList()
    }

    /**
     * Get news for tickers
     * GET https://api.marketaux.com/v1/news/all?symbols={tickers}
     */
    override suspend fun news(ticker: String, limit: Int): List<TickerNewsDto> {
        val (uri, apiKey) = providerConfigService.get(Provider.MARKETAUX)

        try {
            val response: MarketAuxNewsResponse = client.get("$uri/news/all") {
                parameter("api_token", apiKey)
                parameter("symbols", ticker)
                parameter("filter_entities", "true")
                parameter("limit", 3)
            }.body()

            if (response.error != null) {
                return emptyList()
            }

            return response.data?.map { newsItem ->
                convertToTickerNewsDto(newsItem)
            } ?: emptyList()

        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun convertToTickerNewsDto(newsItem: MarketAuxNewsItem): TickerNewsDto {
        // Parse published_at format: ISO 8601 (e.g., "2024-01-15T10:30:00.000000Z")
        val timestamp = try {
            val instant = ZonedDateTime.parse(newsItem.publishedAt).toInstant()
            instant.toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        // Format date time for display
        val dateTimeFormatted = try {
            val zonedDateTime = ZonedDateTime.parse(newsItem.publishedAt)
            val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            zonedDateTime.format(displayFormatter)
        } catch (_: Exception) {
            newsItem.publishedAt ?: ""
        }

        return TickerNewsDto(
            title = newsItem.title ?: "",
            provider = newsItem.source,
            dateTimeFormatted = dateTimeFormatted,
            timestamp = timestamp,
            url = newsItem.url,
        )
    }
}
