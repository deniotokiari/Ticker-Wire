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
import pl.deniotokiari.tickerwire.models.external.MarketStackEodResponse
import pl.deniotokiari.tickerwire.models.external.MarketStackTickersListResponse
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import java.util.Locale

/**
 * MarketStack API v2 implementation
 * Documentation: https://docs.apilayer.com/marketstack/docs/marketstack-api-v2-v-2-0-0
 */
class MarketStackStockProvider(
    private val client: HttpClient,
    private val providerConfigService: ProviderConfigService,
) : StockSearchProvider, StockInfoProvider {

    /**
     * Search for tickers using the Tickers List endpoint
     * GET /tickers/list?search={query}
     */
    override suspend fun search(query: String): List<TickerDto> {
        val (uri, apiKey) = providerConfigService.get(Provider.MARKETSTACK)

        val response: MarketStackTickersListResponse = client.get("$uri/tickerslist") {
            parameter("access_key", apiKey)
            parameter("search", query)
            parameter("limit", 25)
        }.body()

        if (response.error != null) {
            throw Exception("MarketStack API error: ${response.error.message}")
        }

        return response.data?.mapNotNull { ticker ->
            if (ticker.ticker != null && ticker.name != null) {
                TickerDto(
                    ticker = ticker.ticker,
                    company = ticker.name,
                )
            } else {
                null
            }
        } ?: emptyList()
    }

    /**
     * Get latest EOD data for tickers
     * GET /eod/latest?symbols={tickers}
     */
    override suspend fun info(tickers: List<String>): Map<String, TickerInfoDto> {
        if (tickers.isEmpty()) {
            return emptyMap()
        }

        val (uri, apiKey) = providerConfigService.get(Provider.MARKETSTACK)

        val response: MarketStackEodResponse = client.get("$uri/eod/latest") {
            parameter("access_key", apiKey)
            parameter("symbols", tickers.joinToString(","))
            parameter("limit", 1000)
        }.body()

        if (response.error != null) {
            throw Exception("MarketStack API error: ${response.error.message}")
        }

        val result = mutableMapOf<String, TickerInfoDto>()

        response.data?.forEach { eod ->
            val symbol = eod.symbol ?: return@forEach
            val close = eod.close ?: return@forEach
            val open = eod.open ?: close

            // Calculate change
            val change = close - open
            val changePercent = if (open != 0.0) (change / open) * 100 else 0.0

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

            result[symbol] = TickerInfoDto(
                marketValueFormatted = String.format(Locale.US, "%.2f", close),
                deltaFormatted = deltaFormatted,
                percentFormatted = percentFormatted,
                currency = "",
            )
        }

        return result
    }
}
