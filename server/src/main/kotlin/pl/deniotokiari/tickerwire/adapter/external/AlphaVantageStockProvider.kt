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
import pl.deniotokiari.tickerwire.models.external.AlphaVantageNewsItem
import pl.deniotokiari.tickerwire.models.external.AlphaVantageNewsResponse
import pl.deniotokiari.tickerwire.models.external.AlphaVantageQuote
import pl.deniotokiari.tickerwire.models.external.AlphaVantageQuoteResponse
import pl.deniotokiari.tickerwire.models.external.AlphaVantageSearchResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlphaVantageStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockNewsProvider, StockInfoProvider {
    override suspend fun search(query: String): List<TickerDto> {
        val (uri, apiKey) = providerConfigService.get(Provider.ALPHAVANTAGE)

        val response: AlphaVantageSearchResponse = client.get(uri) {
            parameter("function", "SYMBOL_SEARCH")
            parameter("keywords", query)
            parameter("apikey", apiKey)
        }.body()

        if (response.errorMessage != null) {
            throw Exception("Alpha Vantage API error: ${response.errorMessage}")
        }

        if (response.note != null) {
            throw Exception("Alpha Vantage API note: ${response.note}")
        }

        return response.bestMatches?.map { match ->
            TickerDto(
                ticker = match.symbol,
                company = match.name,
            )
        } ?: emptyList()
    }

    override suspend fun news(tickers: List<String>): Map<String, List<TickerNewsDto>> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val (uri, apiKey) = providerConfigService.get(Provider.ALPHAVANTAGE)

        val newsByTicker = mutableMapOf<String, List<TickerNewsDto>>()

        tickers.forEach { ticker ->
            try {
                val response: AlphaVantageNewsResponse = client.get(uri) {
                    parameter("function", "NEWS_SENTIMENT")
                    parameter("tickers", ticker)
                    parameter("limit", "10")
                    parameter("apikey", apiKey)
                }.body()

                if (response.errorMessage != null) {
                    newsByTicker[ticker] = emptyList()

                    return@forEach
                }

                if (response.note != null) {
                    newsByTicker[ticker] = emptyList()

                    return@forEach
                }

                val newsItems = response.feed
                    ?.filter { newsItem -> newsItem.tickerSentiment?.any { it.ticker == ticker } == true }
                    ?.map { newsItem -> convertToTickerNewsDto(newsItem) }
                    ?.take(10)
                    ?: emptyList()

                newsByTicker[ticker] = newsItems
            } catch (_: Exception) {
                newsByTicker[ticker] = emptyList()
            }
        }

        return newsByTicker
    }

    override suspend fun info(tickers: List<String>): Map<String, TickerInfoDto> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val (uri, apiKey) = providerConfigService.get(Provider.ALPHAVANTAGE)

        val infoByTicker = mutableMapOf<String, TickerInfoDto>()

        tickers.forEach { ticker ->
            try {
                val response: AlphaVantageQuoteResponse = client.get(uri) {
                    parameter("function", "GLOBAL_QUOTE")
                    parameter("symbol", ticker)
                    parameter("apikey", apiKey)
                }.body()

                if (response.errorMessage != null) {
                    return@forEach
                }

                if (response.note != null) {
                    return@forEach
                }

                val quote = response.globalQuote

                if (quote != null) {
                    val tickerInfo = convertToTickerInfoDto(quote)
                    infoByTicker[ticker] = tickerInfo
                }
            } catch (_: Exception) {
                // Continue with other tickers even if one fails
            }
        }

        return infoByTicker
    }

    private fun convertToTickerInfoDto(quote: AlphaVantageQuote): TickerInfoDto {
        // Extract price (market value)
        val marketValue = quote.price
        val marketValueFormatted = marketValue.dropLast(2)

        // Extract change (delta)
        val change = quote.change.toDoubleOrNull() ?: 0.0
        val deltaFormatted = if (change >= 0) {
            "+${String.format(Locale.US, "%.2f", change)}"
        } else {
            String.format(Locale.US, "%.2f", change)
        }

        // Extract change percent
        val changePercent = quote.changePercent.removeSuffix("%").toDoubleOrNull() ?: 0.0
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

    private fun convertToTickerNewsDto(newsItem: AlphaVantageNewsItem): TickerNewsDto {
        // Parse time_published format: "20240101T120000"
        // Format: YYYYMMDDTHHMMSS
        val timestamp = try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            val instant = java.time.LocalDateTime.parse(newsItem.timePublished, formatter)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
            instant.toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }

        // Format date time for display (e.g., "2024-01-01 12:00:00")
        val dateTimeFormatted = try {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
            val dateTime = java.time.LocalDateTime.parse(newsItem.timePublished, formatter)
            val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            dateTime.format(displayFormatter)
        } catch (_: Exception) {
            newsItem.timePublished
        }

        return TickerNewsDto(
            title = newsItem.title,
            provider = newsItem.source,
            dateTimeFormatted = dateTimeFormatted,
            timestamp = timestamp,
            url = newsItem.url,
        )
    }
}
