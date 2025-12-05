package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto

interface StockNewsProvider {
    suspend fun news(tickers: List<String>): Map<String, List<TickerNewsDto>>
}
