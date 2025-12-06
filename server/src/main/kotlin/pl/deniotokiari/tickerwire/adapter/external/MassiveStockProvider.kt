package pl.deniotokiari.tickerwire.adapter.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import pl.deniotokiari.tickerwire.adapter.StockInfoProvider
import pl.deniotokiari.tickerwire.adapter.StockNewsProvider
import pl.deniotokiari.tickerwire.adapter.StockSearchProvider
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.external.MassiveNewsResponse
import pl.deniotokiari.tickerwire.models.external.MassivePrevCloseResponse
import pl.deniotokiari.tickerwire.models.external.MassiveTickersResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Massive Stock Provider
 * Documentation: https://massive.com/docs/rest/stocks/overview
 *
 * Free tier: 5 API calls per minute
 *
 * Endpoints used:
 * - Search: /v3/reference/tickers?search={query}
 * - Info: /v2/aggs/ticker/{ticker}/prev
 * - News: /v3/reference/news?ticker={ticker}
 *
 * Note: API uses Bearer token authentication
 */
class MassiveStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockNewsProvider, StockInfoProvider {

    override suspend fun search(query: String): List<TickerDto> {
        val config = providerConfigService.get(Provider.MASSIVE)

        val response: MassiveTickersResponse = client.get("${config.apiUri}/v3/reference/tickers") {
            parameter("search", query)
            parameter("active", true)
            parameter("market", "stocks")
            parameter("limit", 50)
            header("Authorization", "Bearer ${config.apiKey}")
        }.body()

        return response.results?.mapNotNull { ticker ->
            val symbol = ticker.ticker ?: return@mapNotNull null
            val name = ticker.name ?: return@mapNotNull null

            TickerDto(
                ticker = symbol,
                company = name,
            )
        } ?: emptyList()
    }

    override suspend fun news(tickers: Collection<String>): Map<String, List<TickerNewsDto>> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val config = providerConfigService.get(Provider.MASSIVE)
        val newsByTicker = mutableMapOf<String, List<TickerNewsDto>>()

        tickers.take(1).forEach { ticker ->
            try {
                val response: MassiveNewsResponse = client.get("${config.apiUri}/v2/reference/news") {
                    parameter("ticker", ticker)
                    parameter("limit", 10)
                    header("Authorization", "Bearer ${config.apiKey}")
                }.body()

                val newsItems = response.results?.mapNotNull { newsItem ->
                    convertToTickerNewsDto(newsItem)
                }?.take(10) ?: emptyList()

                newsByTicker[ticker] = newsItems
            } catch (_: Exception) {
                newsByTicker[ticker] = emptyList()
            }
        }

        return newsByTicker
    }

    override suspend fun info(tickers: Collection<String>): Map<String, TickerInfoDto> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val config = providerConfigService.get(Provider.MASSIVE)
        val infoByTicker = mutableMapOf<String, TickerInfoDto>()

        // Use previous close endpoint for each ticker
        tickers.take(1).forEach { ticker ->
            try {
                val response: MassivePrevCloseResponse =
                    client.get("${config.apiUri}/v2/aggs/ticker/$ticker/prev") {
                        header("Authorization", "Bearer ${config.apiKey}")
                    }.body()

                val prevBar = response.results?.firstOrNull()
                if (prevBar != null) {
                    val tickerInfo = convertToTickerInfoDto(prevBar)
                    if (tickerInfo != null) {
                        infoByTicker[ticker] = tickerInfo
                    }
                }
            } catch (_: Exception) {
                // Continue with other tickers
            }
        }

        return infoByTicker
    }

    private fun convertToTickerInfoDto(prevBar: pl.deniotokiari.tickerwire.models.external.MassivePrevBar): TickerInfoDto? {
        val closePrice = prevBar.c ?: return null
        val openPrice = prevBar.o ?: closePrice

        val change = closePrice - openPrice
        val changePercent = if (openPrice != 0.0) (change / openPrice) * 100 else 0.0

        val marketValueFormatted = String.format(Locale.US, "%.2f", closePrice)

        val deltaFormatted = if (change >= 0) {
            "+${String.format(Locale.US, "%.2f", change)}"
        } else {
            String.format(Locale.US, "%.2f", change)
        }

        val percentFormatted = if (changePercent >= 0) {
            "+${String.format(Locale.US, "%.2f", changePercent)}%"
        } else {
            "${String.format(Locale.US, "%.2f", changePercent)}%"
        }

        return TickerInfoDto(
            marketValueFormatted = marketValueFormatted,
            deltaFormatted = deltaFormatted,
            percentFormatted = percentFormatted,
            currency = "",
        )
    }

    private fun convertToTickerNewsDto(newsItem: pl.deniotokiari.tickerwire.models.external.MassiveNewsItem): TickerNewsDto? {
        val title = newsItem.title ?: return null
        val publishedUtc = newsItem.publishedUtc ?: return null

        // Parse ISO 8601 datetime: "2024-01-15T10:30:00Z"
        val timestamp = try {
            Instant.parse(publishedUtc).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        // Format date time for display
        val dateTimeFormatted = try {
            val instant = Instant.parse(publishedUtc)
            val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            dateTime.format(displayFormatter)
        } catch (_: Exception) {
            publishedUtc
        }

        return TickerNewsDto(
            title = title,
            provider = newsItem.publisher?.name,
            dateTimeFormatted = dateTimeFormatted,
            timestamp = timestamp,
            url = newsItem.articleUrl,
        )
    }
}
