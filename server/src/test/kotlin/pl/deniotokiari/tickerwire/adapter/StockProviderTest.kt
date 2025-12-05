package pl.deniotokiari.tickerwire.adapter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.LimitUsage
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig
import pl.deniotokiari.tickerwire.services.FirestoreLimitUsageService
import pl.deniotokiari.tickerwire.services.ProviderConfigService
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService
import pl.deniotokiari.tickerwire.services.cache.FirestoreCacheService
import java.time.LocalDateTime

class StockProviderTest : BehaviorSpec({

    // Helper to create mock stats service
    fun createMockStatsService(): ProviderStatsService {
        val mock = mockk<ProviderStatsService>()
        coJustRun { mock.recordSelection(any()) }
        coJustRun { mock.recordFailure(any()) }
        return mock
    }

    // Helper to create mock caches
    fun createMockSearchCache(): FirestoreCacheService<List<TickerDto>> {
        val mock = mockk<FirestoreCacheService<List<TickerDto>>>()
        coEvery { mock.get(any()) } returns null
        coJustRun { mock.put(any(), any()) }
        return mock
    }

    fun createMockNewsCache(): FirestoreCacheService<List<TickerNewsDto>> {
        val mock = mockk<FirestoreCacheService<List<TickerNewsDto>>>()
        coEvery { mock.get(any()) } returns null
        coJustRun { mock.put(any(), any()) }
        return mock
    }

    fun createMockInfoCache(): FirestoreCacheService<TickerInfoDto> {
        val mock = mockk<FirestoreCacheService<TickerInfoDto>>()
        coEvery { mock.get(any()) } returns null
        coJustRun { mock.put(any(), any()) }
        return mock
    }

    val testConfig = ProviderConfig(
        apiUri = "https://api.test.com",
        apiKey = "test-key",
        limit = LimitConfig(perDay = 100)
    )

    Given("StockProvider search function") {

        When("provider is available and has capacity") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 10
            )

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit)
            } returns LimitUsage(usedCount = 11)

            coEvery { mockSearchProvider.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc")
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should return search results") {
                val results = stockProvider.search("AAPL")
                results shouldHaveSize 1
                results[0].ticker shouldBe "AAPL"
            }

            Then("should increment usage") {
                stockProvider.search("AAPL")
                coVerify { mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit) }
            }
        }

        When("provider has reached its limit") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 10
            )

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit)
            } returns null

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should throw NoAvailableProviderException") {
                val exception = shouldThrow<NoAvailableProviderException> {
                    stockProvider.search("AAPL")
                }
                exception.message shouldContain "reached its limit"
            }
        }

        When("no providers are configured") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            every { mockProviderConfigService.configs } returns emptyMap()

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should throw NoAvailableProviderException") {
                val exception = shouldThrow<NoAvailableProviderException> {
                    stockProvider.search("AAPL")
                }
                exception.message shouldContain "All providers have reached their limits"
            }
        }

        When("provider usage check shows no capacity") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 100 // At limit
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should throw NoAvailableProviderException") {
                shouldThrow<NoAvailableProviderException> {
                    stockProvider.search("AAPL")
                }
            }
        }
    }

    Given("StockProvider news function") {

        When("provider is available") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockNewsProvider = mockk<StockNewsProvider>()

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                usedCount = 5
            )

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit)
            } returns LimitUsage(usedCount = 6)

            coEvery { mockNewsProvider.news(listOf("AAPL")) } returns mapOf(
                "AAPL" to listOf(
                    TickerNewsDto(
                        title = "Apple news",
                        provider = "TechNews",
                        dateTimeFormatted = "2024-01-15 12:00:00",
                        timestamp = 1705320000000,
                        url = "https://example.com/news/1",
                    )
                )
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(Provider.ALPHAVANTAGE to mockNewsProvider),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should return news") {
                val results = stockProvider.news(listOf("AAPL"))
                results.containsKey("AAPL") shouldBe true
                results["AAPL"]!! shouldHaveSize 1
            }
        }
    }

    Given("StockProvider info function") {

        When("provider is available") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockInfoProvider = mockk<StockInfoProvider>()

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                usedCount = 5
            )

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit)
            } returns LimitUsage(usedCount = 6)

            coEvery { mockInfoProvider.info(listOf("AAPL")) } returns mapOf(
                "AAPL" to TickerInfoDto(
                    marketValueFormatted = "150.00",
                    deltaFormatted = "+2.00",
                    percentFormatted = "+1.35%",
                    currency = "USD"
                )
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = emptyMap(),
                infoProviders = mapOf(Provider.ALPHAVANTAGE to mockInfoProvider),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should return info") {
                val results = stockProvider.info(listOf("AAPL"))
                results.containsKey("AAPL") shouldBe true
                results["AAPL"]!!.marketValueFormatted shouldBe "150.00"
            }
        }
    }

    Given("StockProvider with multiple providers") {

        When("first provider is at limit but second is available") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockSearchProvider2 = mockk<StockSearchProvider>()

            val config1 = testConfig.copy(limit = LimitConfig(perDay = 10))
            val config2 = testConfig.copy(limit = LimitConfig(perDay = 100))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to config1,
                Provider.MARKETSTACK to config2
            )

            // AlphaVantage is at limit
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 10
            )

            // MarketStack has capacity
            coEvery { mockLimitUsageService.getUsage(Provider.MARKETSTACK) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 5
            )

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.MARKETSTACK, config2.limit)
            } returns LimitUsage(usedCount = 6)

            coEvery { mockSearchProvider2.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc")
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(
                    Provider.ALPHAVANTAGE to mockSearchProvider,
                    Provider.MARKETSTACK to mockSearchProvider2
                ),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should use the available provider") {
                val results = stockProvider.search("AAPL")
                results shouldHaveSize 1
            }

            Then("should call the available provider") {
                stockProvider.search("AAPL")
                coVerify { mockSearchProvider2.search("AAPL") }
            }
        }

        When("all providers are at limit") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            val config1 = testConfig.copy(limit = LimitConfig(perDay = 10))
            val config2 = testConfig.copy(limit = LimitConfig(perDay = 10))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to config1,
                Provider.MARKETSTACK to config2
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 10
            )

            coEvery { mockLimitUsageService.getUsage(Provider.MARKETSTACK) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 10
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(
                    Provider.ALPHAVANTAGE to mockSearchProvider,
                    Provider.MARKETSTACK to mockk()
                ),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should throw NoAvailableProviderException") {
                shouldThrow<NoAvailableProviderException> {
                    stockProvider.search("AAPL")
                }
            }
        }
    }

    Given("StockProvider priority-based selection") {

        When("FINNHUB has higher priority than ALPHAVANTAGE for search") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockFinnhubProvider = mockk<StockSearchProvider>()
            val mockAlphaVantageProvider = mockk<StockSearchProvider>()

            val finnhubConfig = testConfig.copy(limit = LimitConfig(perMinute = 60))
            val alphaVantageConfig = testConfig.copy(limit = LimitConfig(perDay = 25))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.FINNHUB to finnhubConfig,
                Provider.ALPHAVANTAGE to alphaVantageConfig
            )

            // Both have capacity
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 5)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 5)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.FINNHUB, finnhubConfig.limit)
            } returns LimitUsage(usedCount = 6)

            coEvery { mockFinnhubProvider.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc")
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(
                    Provider.FINNHUB to mockFinnhubProvider,
                    Provider.ALPHAVANTAGE to mockAlphaVantageProvider
                ),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should always select FINNHUB (higher priority)") {
                stockProvider.search("AAPL")
                coVerify(exactly = 1) { mockFinnhubProvider.search("AAPL") }
                coVerify(exactly = 0) { mockAlphaVantageProvider.search(any()) }
            }
        }

        When("high priority provider is at limit, falls back to lower priority") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockFinnhubProvider = mockk<StockSearchProvider>()
            val mockMassiveProvider = mockk<StockSearchProvider>()

            val finnhubConfig = testConfig.copy(limit = LimitConfig(perMinute = 60))
            val massiveConfig = testConfig.copy(limit = LimitConfig(perMinute = 5))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.FINNHUB to finnhubConfig,
                Provider.MASSIVE to massiveConfig
            )

            // FINNHUB is at limit
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(
                lastUsed = LocalDateTime.now(),
                usedCount = 60
            )

            // MASSIVE has capacity
            coEvery { mockLimitUsageService.getUsage(Provider.MASSIVE) } returns LimitUsage(usedCount = 2)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.MASSIVE, massiveConfig.limit)
            } returns LimitUsage(usedCount = 3)

            coEvery { mockMassiveProvider.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc")
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(
                    Provider.FINNHUB to mockFinnhubProvider,
                    Provider.MASSIVE to mockMassiveProvider
                ),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should fall back to MASSIVE") {
                stockProvider.search("AAPL")
                coVerify(exactly = 0) { mockFinnhubProvider.search(any()) }
                coVerify(exactly = 1) { mockMassiveProvider.search("AAPL") }
            }
        }

        When("same priority providers, prefer one with more capacity") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockStockDataProvider = mockk<StockSearchProvider>()
            val mockMarketAuxProvider = mockk<StockSearchProvider>()

            // Both are Tier 2 providers with same daily limit
            val stockDataConfig = testConfig.copy(limit = LimitConfig(perDay = 100))
            val marketAuxConfig = testConfig.copy(limit = LimitConfig(perDay = 100))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.STOCKDATA to stockDataConfig,
                Provider.MARKETAUX to marketAuxConfig
            )

            // StockData has more remaining capacity (90 remaining)
            coEvery { mockLimitUsageService.getUsage(Provider.STOCKDATA) } returns LimitUsage(usedCount = 10)

            // MarketAux has less remaining capacity (20 remaining)
            coEvery { mockLimitUsageService.getUsage(Provider.MARKETAUX) } returns LimitUsage(usedCount = 80)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.STOCKDATA, stockDataConfig.limit)
            } returns LimitUsage(usedCount = 11)

            coEvery { mockStockDataProvider.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc")
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(
                    Provider.STOCKDATA to mockStockDataProvider,
                    Provider.MARKETAUX to mockMarketAuxProvider
                ),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should select StockData (more capacity at same priority)") {
                stockProvider.search("AAPL")
                coVerify(exactly = 1) { mockStockDataProvider.search("AAPL") }
                coVerify(exactly = 0) { mockMarketAuxProvider.search(any()) }
            }
        }

        When("news priority is different from search priority") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockFinnhubProvider = mockk<StockNewsProvider>()
            val mockStockDataProvider = mockk<StockNewsProvider>()

            val finnhubConfig = testConfig.copy(limit = LimitConfig(perMinute = 60))
            val stockDataConfig = testConfig.copy(limit = LimitConfig(perDay = 100))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.FINNHUB to finnhubConfig,
                Provider.STOCKDATA to stockDataConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 5)
            coEvery { mockLimitUsageService.getUsage(Provider.STOCKDATA) } returns LimitUsage(usedCount = 5)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.FINNHUB, finnhubConfig.limit)
            } returns LimitUsage(usedCount = 6)

            coEvery { mockFinnhubProvider.news(listOf("AAPL")) } returns mapOf(
                "AAPL" to listOf(
                    TickerNewsDto(
                        title = "FinnHub News",
                        provider = "FinnHub",
                        dateTimeFormatted = "2024-01-15",
                        timestamp = 1705320000000,
                        url = "https://example.com/news/2",
                    )
                )
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(
                    Provider.FINNHUB to mockFinnhubProvider,
                    Provider.STOCKDATA to mockStockDataProvider
                ),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should select FINNHUB for news (highest priority in NEWS_PRIORITY)") {
                stockProvider.news(listOf("AAPL"))
                coVerify(exactly = 1) { mockFinnhubProvider.news(listOf("AAPL")) }
                coVerify(exactly = 0) { mockStockDataProvider.news(any()) }
            }
        }

        When("info priority prefers FMP over MARKETSTACK") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockFmpProvider = mockk<StockInfoProvider>()
            val mockMarketStackProvider = mockk<StockInfoProvider>()

            val fmpConfig = testConfig.copy(limit = LimitConfig(perDay = 250))
            val marketStackConfig = testConfig.copy(limit = LimitConfig(perMonth = 100))

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.FINANCIALMODELINGPREP to fmpConfig,
                Provider.MARKETSTACK to marketStackConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.FINANCIALMODELINGPREP) } returns LimitUsage(usedCount = 50)
            coEvery { mockLimitUsageService.getUsage(Provider.MARKETSTACK) } returns LimitUsage(usedCount = 10)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.FINANCIALMODELINGPREP, fmpConfig.limit)
            } returns LimitUsage(usedCount = 51)

            coEvery { mockFmpProvider.info(listOf("AAPL")) } returns mapOf(
                "AAPL" to TickerInfoDto(
                    marketValueFormatted = "150.00",
                    deltaFormatted = "+2.00",
                    percentFormatted = "+1.35%",
                    currency = "USD"
                )
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = emptyMap(),
                infoProviders = mapOf(
                    Provider.FINANCIALMODELINGPREP to mockFmpProvider,
                    Provider.MARKETSTACK to mockMarketStackProvider
                ),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should select FMP (priority 3) over MarketStack (priority 5)") {
                stockProvider.info(listOf("AAPL"))
                coVerify(exactly = 1) { mockFmpProvider.info(listOf("AAPL")) }
                coVerify(exactly = 0) { mockMarketStackProvider.info(any()) }
            }
        }
    }

    Given("StockProvider cache behavior") {

        When("search result is cached") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockSearchCache = mockk<FirestoreCacheService<List<TickerDto>>>()

            val cachedResults = listOf(TickerDto(ticker = "AAPL", company = "Apple Inc"))

            // Cache returns cached value via get()
            coEvery { mockSearchCache.get(any()) } returns cachedResults

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = mockSearchCache,
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should return cached results without calling provider") {
                val results = stockProvider.search("AAPL")
                results shouldHaveSize 1
                results[0].ticker shouldBe "AAPL"

                // Provider should not be called since cache returned value
                coVerify(exactly = 0) { mockSearchProvider.search(any()) }
            }
        }

        When("news is partially cached") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockNewsProvider = mockk<StockNewsProvider>()
            val mockNewsCache = mockk<FirestoreCacheService<List<TickerNewsDto>>>()

            val cachedAaplNews = listOf(
                TickerNewsDto(
                    title = "Cached Apple news",
                    provider = "Cache",
                    dateTimeFormatted = "2024-01-15",
                    timestamp = 1705320000000,
                    url = "https://example.com/cached/news",
                )
            )

            // AAPL is cached, MSFT is not
            coEvery { mockNewsCache.get("AAPL") } returns cachedAaplNews
            coEvery { mockNewsCache.get("MSFT") } returns null
            coJustRun { mockNewsCache.put(any(), any()) }

            every { mockProviderConfigService.configs } returns mapOf(
                Provider.ALPHAVANTAGE to testConfig
            )

            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 5)

            coEvery {
                mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit)
            } returns LimitUsage(usedCount = 6)

            // Provider only fetches MSFT
            coEvery { mockNewsProvider.news(listOf("MSFT")) } returns mapOf(
                "MSFT" to listOf(
                    TickerNewsDto(
                        title = "MSFT news",
                        provider = "Provider",
                        dateTimeFormatted = "2024-01-15",
                        timestamp = 1705320000000,
                        url = "https://example.com/news/msft",
                    )
                )
            )

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(Provider.ALPHAVANTAGE to mockNewsProvider),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = mockNewsCache,
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            Then("should return both cached and fetched news") {
                val results = stockProvider.news(listOf("AAPL", "MSFT"))
                results.size shouldBe 2
                results["AAPL"]!![0].title shouldBe "Cached Apple news"
                results["MSFT"]!![0].title shouldBe "MSFT news"
            }

            Then("should only fetch non-cached tickers") {
                stockProvider.news(listOf("AAPL", "MSFT"))
                coVerify { mockNewsProvider.news(listOf("MSFT")) }
            }
        }
    }
})
