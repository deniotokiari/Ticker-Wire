package pl.deniotokiari.tickerwire.feature.home.data.source

import kotlinx.serialization.serializer
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.store.KeyValueLocalDataSource
import pl.deniotokiari.tickerwire.model.Ticker

private const val NAME_WATCH_LIST = "watch_list"
private const val KEY_ITEMS = "items"

@Single
class WatchlistLocalDataSource(
    private val keyValueLocalDataSource: KeyValueLocalDataSource = KeyValueLocalDataSource(name = NAME_WATCH_LIST),
) {
    fun addTicker(ticker: Ticker) {
        val tickers = getTickers()

        keyValueLocalDataSource.setValue(
            key = KEY_ITEMS,
            value = tickers + ticker,
            kSerializer = serializer<List<Ticker>>(),
        )
    }

    fun removeTicker(ticker: Ticker) {
        val tickers = getTickers()

        keyValueLocalDataSource.setValue(
            key = KEY_ITEMS,
            value = tickers - ticker,
            kSerializer = serializer<List<Ticker>>(),
        )
    }

    fun getTickers(): List<Ticker> {
        return keyValueLocalDataSource.getValue(
            key = KEY_ITEMS,
            kSerializer = serializer<List<Ticker>>(),
        ) ?: emptyList()
    }
}
