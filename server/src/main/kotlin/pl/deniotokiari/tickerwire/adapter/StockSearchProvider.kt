package pl.deniotokiari.tickerwire.adapter

import pl.deniotokiari.tickerwire.model.dto.TickerDto

interface StockSearchProvider {
    suspend fun search(query: String): List<TickerDto>
}
