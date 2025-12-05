package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto

interface StockInfoProvider {
    suspend fun info(tickers: List<String>): Map<String, TickerInfoDto>
}
