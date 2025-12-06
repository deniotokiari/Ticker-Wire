package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto

interface StockInfoProvider {
    suspend fun info(tickers: Collection<String>): Map<String, TickerInfoDto>
}
