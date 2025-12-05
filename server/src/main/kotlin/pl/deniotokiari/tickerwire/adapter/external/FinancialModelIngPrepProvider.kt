package pl.deniotokiari.tickerwire.adapter.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pl.deniotokiari.tickerwire.adapter.StockInfoProvider
import pl.deniotokiari.tickerwire.adapter.StockSearchProvider
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.external.FmpQuote
import pl.deniotokiari.tickerwire.models.external.FmpSearchResult
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.util.Locale

/**
 * Financial Modeling Prep Stock Provider
 * Documentation: https://site.financialmodelingprep.com/developer/docs
 *
 * Free tier: 250 API calls per day
 *
 * Endpoints used:
 * - Search: /stable/search-symbol?query=AAPL
 * - Quote: /stable/quote/AAPL
 *
 * Note: News endpoint requires paid plan
 */
class FinancialModelIngPrepProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockInfoProvider {

    override suspend fun search(query: String): List<TickerDto> {
        val config = providerConfigService.get(Provider.FINANCIALMODELINGPREP)

        val response: List<FmpSearchResult> = client.get("${config.apiUri}/search-symbol") {
            parameter("query", query)
            parameter("apikey", config.apiKey)
        }.body()

        return response.mapNotNull { result ->
            val symbol = result.symbol ?: return@mapNotNull null
            val name = result.name ?: return@mapNotNull null

            TickerDto(
                ticker = symbol,
                company = name,
            )
        }
    }

    override suspend fun info(tickers: List<String>): Map<String, TickerInfoDto> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val config = providerConfigService.get(Provider.FINANCIALMODELINGPREP)

        val infoByTicker = mutableMapOf<String, TickerInfoDto>()

        // FMP supports bulk quote requests with comma-separated symbols
        try {
            val symbolsParam = tickers.joinToString(",")
            val response: List<FmpQuote> = client.get("${config.apiUri}/quote/$symbolsParam") {
                parameter("apikey", config.apiKey)
            }.body()

            response.forEach { quote ->
                val symbol = quote.symbol ?: return@forEach
                val tickerInfo = convertToTickerInfoDto(quote)
                infoByTicker[symbol] = tickerInfo
            }
        } catch (_: Exception) {
            // Fallback to individual requests if bulk fails
            tickers.forEach { ticker ->
                try {
                    val response: List<FmpQuote> = client.get("${config.apiUri}/quote/$ticker") {
                        parameter("apikey", config.apiKey)
                    }.body()

                    val quote = response.firstOrNull() ?: return@forEach
                    val tickerInfo = convertToTickerInfoDto(quote)
                    infoByTicker[ticker] = tickerInfo
                } catch (_: Exception) {
                    // Continue with other tickers
                }
            }
        }

        return infoByTicker
    }

    private fun convertToTickerInfoDto(quote: FmpQuote): TickerInfoDto {
        val price = quote.price ?: 0.0
        val change = quote.change ?: 0.0
        val changePercent = quote.changesPercentage ?: 0.0

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
}
