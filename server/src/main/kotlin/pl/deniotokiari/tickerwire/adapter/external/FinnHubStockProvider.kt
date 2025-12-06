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
import pl.deniotokiari.tickerwire.models.external.FinnhubNewsItem
import pl.deniotokiari.tickerwire.models.external.FinnhubQuote
import pl.deniotokiari.tickerwire.models.external.FinnhubSearchResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Finnhub Stock Provider
 * Documentation: https://finnhub.io/docs/api
 *
 * Free tier: 60 API calls per minute
 *
 * Endpoints used:
 * - Search: /api/v1/search?q={query}
 * - Quote: /api/v1/quote?symbol={symbol}
 * - News: /api/v1/company-news?symbol={symbol}&from={date}&to={date}
 */
class FinnHubStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockNewsProvider, StockInfoProvider {

    override suspend fun search(query: String): List<TickerDto> {
        val config = providerConfigService.get(Provider.FINNHUB)

        val response: FinnhubSearchResponse = client.get("${config.apiUri}/search") {
            parameter("q", query)
            parameter("token", config.apiKey)
        }.body()

        return response.result?.mapNotNull { result ->
            val symbol = result.symbol ?: return@mapNotNull null
            val description = result.description ?: return@mapNotNull null

            TickerDto(
                ticker = symbol,
                company = description,
            )
        } ?: emptyList()
    }

    override suspend fun news(tickers: Collection<String>): Map<String, List<TickerNewsDto>> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val config = providerConfigService.get(Provider.FINNHUB)
        val newsByTicker = mutableMapOf<String, List<TickerNewsDto>>()

        // Get news from the last 7 days
        val today = LocalDate.now()
        val fromDate = today.minusDays(7)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        tickers.take(1).forEach { ticker ->
            try {
                val response: List<FinnhubNewsItem> = client.get("${config.apiUri}/company-news") {
                    parameter("symbol", ticker)
                    parameter("from", fromDate.format(dateFormatter))
                    parameter("to", today.format(dateFormatter))
                    parameter("token", config.apiKey)
                }.body()

                val newsItems = response.mapNotNull { newsItem ->
                    convertToTickerNewsDto(newsItem)
                }.take(10)

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

        val config = providerConfigService.get(Provider.FINNHUB)
        val infoByTicker = mutableMapOf<String, TickerInfoDto>()

        // Finnhub quote endpoint only supports one symbol at a time
        tickers.take(1).forEach { ticker ->
            try {
                val response: FinnhubQuote = client.get("${config.apiUri}/quote") {
                    parameter("symbol", ticker)
                    parameter("token", config.apiKey)
                }.body()

                val tickerInfo = convertToTickerInfoDto(response)
                if (tickerInfo != null) {
                    infoByTicker[ticker] = tickerInfo
                }
            } catch (_: Exception) {
                // Continue with other tickers
            }
        }

        return infoByTicker
    }

    private fun convertToTickerInfoDto(quote: FinnhubQuote): TickerInfoDto? {
        val price = quote.c ?: return null
        val change = quote.d ?: 0.0
        val changePercent = quote.dp ?: 0.0

        val marketValueFormatted = String.format(Locale.US, "%.2f", price)

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

    private fun convertToTickerNewsDto(newsItem: FinnhubNewsItem): TickerNewsDto? {
        val headline = newsItem.headline ?: return null
        val datetime = newsItem.datetime ?: return null

        // datetime is Unix timestamp in seconds
        val timestamp = datetime * 1000 // Convert to milliseconds

        // Format date time for display
        val dateTimeFormatted = try {
            val instant = Instant.ofEpochSecond(datetime)
            val dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            dateTime.format(displayFormatter)
        } catch (_: Exception) {
            Instant.ofEpochSecond(datetime).toString()
        }

        return TickerNewsDto(
            title = headline,
            provider = newsItem.source,
            dateTimeFormatted = dateTimeFormatted,
            timestamp = timestamp,
            url = newsItem.url,
        )
    }
}
