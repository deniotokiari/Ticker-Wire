package pl.deniotokiari.tickerwire.adapter.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pl.deniotokiari.tickerwire.adapter.StockInfoProvider
import pl.deniotokiari.tickerwire.adapter.StockNewsProvider
import pl.deniotokiari.tickerwire.adapter.StockSearchProvider
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.external.StockDataNewsResponse
import pl.deniotokiari.tickerwire.models.external.StockDataQuoteResponse
import pl.deniotokiari.tickerwire.models.external.StockDataSearchResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * StockData.org API implementation
 * Documentation: https://www.stockdata.org/documentation
 *
 * Provides:
 * - Stock Search: GET /v1/entity/search
 * - Stock Quote: GET /v1/data/quote
 * - News: GET /v1/news/all
 */
class StockDataStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockNewsProvider, StockInfoProvider {

    /**
     * Search for tickers
     * GET https://api.stockdata.org/v1/data/search?search={query}
     */
    override suspend fun search(query: String): List<TickerDto> {
        val (uri, apiKey) = providerConfigService.get(Provider.STOCKDATA)

        val response: StockDataSearchResponse = client.get("$uri/entity/search") {
            parameter("api_token", apiKey)
            parameter("search", query)
        }.body()

        if (response.error != null) {
            throw Exception("StockData API error: ${response.error.message}")
        }

        return response.data?.mapNotNull { item ->
            if (item.ticker != null && item.name != null) {
                TickerDto(
                    ticker = item.ticker,
                    company = item.name,
                )
            } else {
                null
            }
        } ?: emptyList()
    }

    /**
     * Get news for tickers
     * GET https://api.stockdata.org/v1/news/all?symbols={tickers}
     */
    override suspend fun news(tickers: Collection<String>): Map<String, List<TickerNewsDto>> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val (uri, apiKey) = providerConfigService.get(Provider.STOCKDATA)

        val newsByTicker = mutableMapOf<String, MutableList<TickerNewsDto>>()
        
        // Initialize empty lists for all tickers
        tickers.take(1).forEach { ticker ->
            newsByTicker[ticker] = mutableListOf()
        }

        try {
            val response: StockDataNewsResponse = client.get("$uri/news/all") {
                parameter("api_token", apiKey)
                parameter("symbols", newsByTicker.keys.joinToString(","))
                parameter("filter_entities", "true")
                parameter("limit", 3)
            }.body()

            if (response.error != null) {
                return newsByTicker.mapValues { it.value.toList() }
            }

            response.data?.forEach { newsItem ->
                val newsDto = convertToTickerNewsDto(newsItem)
                
                // Associate news with relevant tickers from entities
                newsItem.entities?.forEach { entity ->
                    val symbol = entity.symbol
                    if (symbol != null && newsByTicker.contains(symbol)) {
                        newsByTicker[symbol]?.add(newsDto)
                    }
                }
            }

            // Limit to 10 news items per ticker
            return newsByTicker.mapValues { (_, newsList) ->
                newsList.take(10)
            }
        } catch (_: Exception) {
            return newsByTicker.mapValues { it.value.toList() }
        }
    }

    /**
     * Get stock quotes/info for tickers
     * GET https://api.stockdata.org/v1/data/quote?symbols={tickers}
     */
    override suspend fun info(tickers: Collection<String>): Map<String, TickerInfoDto> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val (uri, apiKey) = providerConfigService.get(Provider.STOCKDATA)

        val response: StockDataQuoteResponse = client.get("$uri/data/quote") {
            parameter("api_token", apiKey)
            parameter("symbols", tickers.joinToString(","))
        }.body()

        if (response.error != null) {
            throw Exception("StockData API error: ${response.error.message}")
        }

        val result = mutableMapOf<String, TickerInfoDto>()

        response.data?.forEach { quote ->
            val ticker = quote.ticker ?: return@forEach
            val price = quote.price ?: return@forEach
            val previousClose = quote.previousClosePrice ?: price
            val dayChange = quote.dayChange ?: 0.0

            // Calculate percent change
            val percentChange = if (previousClose != 0.0) {
                (dayChange / previousClose) * 100
            } else {
                0.0
            }

            val deltaFormatted = if (dayChange >= 0) {
                "+${String.format(Locale.US, "%.2f", dayChange)}"
            } else {
                String.format(Locale.US, "%.2f", dayChange)
            }

            val percentFormatted = if (percentChange >= 0) {
                "+${String.format(Locale.US, "%.2f", percentChange)}%"
            } else {
                "${String.format(Locale.US, "%.2f", percentChange)}%"
            }

            result[ticker] = TickerInfoDto(
                marketValueFormatted = String.format(Locale.US, "%.2f", price),
                deltaFormatted = deltaFormatted,
                percentFormatted = percentFormatted,
                currency = quote.currency ?: "",
            )
        }

        return result
    }

    private fun convertToTickerNewsDto(newsItem: pl.deniotokiari.tickerwire.models.external.StockDataNewsItem): TickerNewsDto {
        // Parse published_at format: ISO 8601 (e.g., "2024-01-01T12:00:00.000000Z")
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
