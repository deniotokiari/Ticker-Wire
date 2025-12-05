package pl.deniotokiari.tickerwire.common.data.store

import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.HttpClient
import pl.deniotokiari.tickerwire.model.Ticker
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto

private const val API_URI = "http://0.0.0.0:8080/api/v1"

@Single
class TickerRemoteDataSource(
    private val httpClient: HttpClient,
) {
    suspend fun search(query: String): List<TickerDto> {
        return httpClient.get(
            url = "$API_URI/tickers/search",
            queryParams = mapOf("query" to query),
        )
    }

    suspend fun news(tickers: List<Ticker>): Map<String, List<TickerNewsDto>> {
        return httpClient.post(
            url = "$API_URI/tickers/news",
            body = tickers.map { ticker -> ticker.symbol },
        )
    }

    suspend fun info(tickers: List<Ticker>): Map<String, TickerInfoDto> {
        return httpClient.post(
            url = "$API_URI/tickers/info",
            body = tickers.map { ticker -> ticker.symbol },
        )
    }
}
