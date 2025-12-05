package pl.deniotokiari.tickerwire.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pl.deniotokiari.tickerwire.adapter.StockProvider
import pl.deniotokiari.tickerwire.adapter.external.AlphaVantageStockProvider
import pl.deniotokiari.tickerwire.adapter.external.FinancialModelIngPrepProvider
import pl.deniotokiari.tickerwire.adapter.external.FinnHubStockProvider
import pl.deniotokiari.tickerwire.adapter.external.MarketAuxStockProvider
import pl.deniotokiari.tickerwire.adapter.external.MarketStackStockProvider
import pl.deniotokiari.tickerwire.adapter.external.MassiveStockProvider
import pl.deniotokiari.tickerwire.adapter.external.StockDataStockProvider
import pl.deniotokiari.tickerwire.models.Provider

val providerModule = module {
    singleOf(::AlphaVantageStockProvider)
    singleOf(::FinancialModelIngPrepProvider)
    singleOf(::FinnHubStockProvider)
    singleOf(::MarketAuxStockProvider)
    singleOf(::MarketStackStockProvider)
    singleOf(::MassiveStockProvider)
    singleOf(::StockDataStockProvider)

    single {
        StockProvider(
            providerConfigService = get(),
            limitUsageService = get(),
            newsProviders = mapOf(
                Provider.STOCKDATA to get<StockDataStockProvider>(),
                Provider.MARKETAUX to get<MarketAuxStockProvider>(),
                Provider.FINNHUB to get<FinnHubStockProvider>(),
                Provider.MASSIVE to get<MassiveStockProvider>(),
                Provider.ALPHAVANTAGE to get<AlphaVantageStockProvider>(),
            ),
            searchProviders = mapOf(
                Provider.MARKETSTACK to get<MarketStackStockProvider>(),
                Provider.STOCKDATA to get<StockDataStockProvider>(),
                Provider.MARKETAUX to get<MarketAuxStockProvider>(),
                Provider.FINANCIALMODELINGPREP to get<FinancialModelIngPrepProvider>(),
                Provider.FINNHUB to get<FinnHubStockProvider>(),
                Provider.MASSIVE to get<MassiveStockProvider>(),
                Provider.ALPHAVANTAGE to get<AlphaVantageStockProvider>(),
            ),
            infoProviders = mapOf(
                Provider.MARKETSTACK to get<MarketStackStockProvider>(),
                Provider.STOCKDATA to get<StockDataStockProvider>(),
                Provider.FINANCIALMODELINGPREP to get<FinancialModelIngPrepProvider>(),
                Provider.FINNHUB to get<FinnHubStockProvider>(),
                Provider.MASSIVE to get<MassiveStockProvider>(),
                Provider.ALPHAVANTAGE to get<AlphaVantageStockProvider>(),
            ),
            // Inject caches from CacheModule
            searchCache = get(CacheQualifiers.SEARCH_CACHE),
            newsCache = get(CacheQualifiers.NEWS_CACHE),
            infoCache = get(CacheQualifiers.INFO_CACHE),
            statsService = get(),
        )
    }
}
