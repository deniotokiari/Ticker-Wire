package pl.deniotokiari.tickerwire.feature.home.data.source

import kotlinx.serialization.serializer
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import pl.deniotokiari.tickerwire.common.data.store.KeyValueLocalDataSource
import pl.deniotokiari.tickerwire.model.TickerNews

private const val NAME_VISITED_TICKER_NEWS = "visited_news"
private const val KEY_ITEMS = "items"

@Single
class VisitedTickerNewsLocalDataSource(
    @Named(NAME_VISITED_TICKER_NEWS) private val keyValueLocalDataSource: KeyValueLocalDataSource,
) {
    val items: Set<TickerNews>
        get() = keyValueLocalDataSource.getValue(
            key = KEY_ITEMS,
            kSerializer = serializer<Set<TickerNews>>(),
        ) ?: emptySet()

    fun addTickerNews(item: TickerNews) {
        keyValueLocalDataSource.setValue(
            key = KEY_ITEMS,
            value = items + item,
            kSerializer = serializer<Set<TickerNews>>(),
        )
    }

    fun removeTickerNews(item: TickerNews) {
        keyValueLocalDataSource.setValue(
            key = KEY_ITEMS,
            value = items - item,
            kSerializer = serializer<Set<TickerNews>>(),
        )
    }
}

@Named(NAME_VISITED_TICKER_NEWS)
@Single
fun provideVisitedTickerNewsKeyValueLocalDataSource(): KeyValueLocalDataSource =
    KeyValueLocalDataSource(name = NAME_VISITED_TICKER_NEWS)
