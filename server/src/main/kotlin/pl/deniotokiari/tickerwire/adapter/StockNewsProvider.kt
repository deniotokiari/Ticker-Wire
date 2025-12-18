package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto

interface StockNewsProvider {
    suspend fun news(ticker: String, limit: Int): List<TickerNewsDto>
}
