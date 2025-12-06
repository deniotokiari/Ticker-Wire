package pl.deniotokiari.tickerwire.adapter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
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

class StockProviderTest : BehaviorSpec({

    val testConfig = ProviderConfig(
        apiUri = "https://test.api",
        apiKey = "test-key",
        limit = LimitConfig(perDay = 100)
    )

    fun createMockSearchCache(): FirestoreCacheService<List<TickerDto>> {
        val mock = mockk<FirestoreCacheService<List<TickerDto>>>(relaxed = true)
        coEvery { mock.get(any()) } returns null
        return mock
    }

    fun createMockNewsCache(): FirestoreCacheService<List<TickerNewsDto>> {
        val mock = mockk<FirestoreCacheService<List<TickerNewsDto>>>(relaxed = true)
        coEvery { mock.get(any()) } returns null
        return mock
    }

    fun createMockInfoCache(): FirestoreCacheService<TickerInfoDto> {
        val mock = mockk<FirestoreCacheService<TickerInfoDto>>(relaxed = true)
        coEvery { mock.get(any()) } returns null
        return mock
    }

    fun createMockStatsService(): ProviderStatsService = mockk {
        coEvery { recordSelection(any()) } returns Unit
        coEvery { recordFailure(any()) } returns Unit
    }

    Given("StockProvider search function") {

        When("cache has the result") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockSearchCache = createMockSearchCache()

            val cachedResult = listOf(TickerDto(ticker = "AAPL", company = "Apple Inc"))
            coEvery { mockSearchCache.get("AAPL") } returns cachedResult

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

            val result = stockProvider.search("AAPL")

            Then("should return cached result without calling provider") {
                result shouldBe cachedResult
                coVerify(exactly = 0) { mockSearchProvider.search(any()) }
            }
        }

        When("cache is empty and provider is available") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockSearchCache = createMockSearchCache()

            every { mockProviderConfigService.configs } returns mapOf(Provider.ALPHAVANTAGE to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockSearchProvider.search("AAPL") } returns listOf(TickerDto(ticker = "AAPL", company = "Apple Inc"))

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

            val result = stockProvider.search("AAPL")

            Then("should call provider and cache the result") {
                result.size shouldBe 1
                result[0].ticker shouldBe "AAPL"
                coVerify(exactly = 1) { mockSearchProvider.search("AAPL") }
                coVerify(exactly = 1) { mockSearchCache.put("AAPL", any()) }
            }
        }

        When("all providers have reached their limits") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()

            val limitReachedConfig = testConfig.copy(limit = LimitConfig(perDay = 10))
            every { mockProviderConfigService.configs } returns mapOf(Provider.ALPHAVANTAGE to limitReachedConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 10)

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

            Then("should throw AllProvidersHaveReachedTheirLimitsException") {
                shouldThrow<AllProvidersHaveReachedTheirLimitsException> {
                    stockProvider.search("AAPL")
                }
            }
        }
    }

    Given("StockProvider news function") {

        When("all tickers are cached") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockNewsProvider = mockk<StockNewsProvider>()
            val mockNewsCache = createMockNewsCache()

            val cachedNews = listOf(TickerNewsDto(title = "News", provider = "Test", dateTimeFormatted = "2024-01-01", timestamp = 0, url = ""))
            coEvery { mockNewsCache.get("AAPL") } returns cachedNews
            coEvery { mockNewsCache.get("MSFT") } returns cachedNews

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(Provider.FINNHUB to mockNewsProvider),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = mockNewsCache,
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            val result = stockProvider.news(listOf("AAPL", "MSFT"))

            Then("should return cached results without calling provider") {
                result.size shouldBe 2
                result.containsKey("AAPL") shouldBe true
                result.containsKey("MSFT") shouldBe true
                coVerify(exactly = 0) { mockNewsProvider.news(any()) }
            }
        }

        When("some tickers are cached, some are not") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockNewsProvider = mockk<StockNewsProvider>()
            val mockNewsCache = createMockNewsCache()

            val cachedNews = listOf(TickerNewsDto(title = "Cached News", provider = "Test", dateTimeFormatted = "2024-01-01", timestamp = 0, url = ""))
            val fetchedNews = listOf(TickerNewsDto(title = "Fetched News", provider = "Test", dateTimeFormatted = "2024-01-01", timestamp = 0, url = ""))

            coEvery { mockNewsCache.get("AAPL") } returns cachedNews
            coEvery { mockNewsCache.get("MSFT") } returns null

            every { mockProviderConfigService.configs } returns mapOf(Provider.FINNHUB to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.FINNHUB, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockNewsProvider.news(any()) } returns mapOf("MSFT" to fetchedNews)

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(Provider.FINNHUB to mockNewsProvider),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = mockNewsCache,
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            val result = stockProvider.news(listOf("AAPL", "MSFT"))

            Then("should return cached and fetched results") {
                result.size shouldBe 2
                result["AAPL"]!![0].title shouldBe "Cached News"
                result["MSFT"]!![0].title shouldBe "Fetched News"
            }
        }

        When("provider returns all requested tickers") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockNewsProvider = mockk<StockNewsProvider>()
            val mockNewsCache = createMockNewsCache()

            val news1 = listOf(TickerNewsDto(title = "News 1", provider = "Test", dateTimeFormatted = "2024-01-01", timestamp = 0, url = ""))
            val news2 = listOf(TickerNewsDto(title = "News 2", provider = "Test", dateTimeFormatted = "2024-01-01", timestamp = 0, url = ""))

            val config = testConfig.copy(limit = LimitConfig(perMinute = 60))

            every { mockProviderConfigService.configs } returns mapOf(Provider.FINNHUB to config)
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.FINNHUB, config.limit) } returns LimitUsage(usedCount = 1)

            // Provider returns both tickers
            coEvery { mockNewsProvider.news(any()) } returns mapOf("AAPL" to news1, "MSFT" to news2)

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = mapOf(Provider.FINNHUB to mockNewsProvider),
                searchProviders = emptyMap(),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = mockNewsCache,
                infoCache = createMockInfoCache(),
                statsService = createMockStatsService(),
            )

            val result = stockProvider.news(listOf("AAPL", "MSFT"))

            Then("should return all fetched data") {
                result.size shouldBe 2
                result.containsKey("AAPL") shouldBe true
                result.containsKey("MSFT") shouldBe true
                result["AAPL"]!![0].title shouldBe "News 1"
                result["MSFT"]!![0].title shouldBe "News 2"
            }
        }
    }

    Given("StockProvider info function") {

        When("all tickers are cached") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockInfoProvider = mockk<StockInfoProvider>()
            val mockInfoCache = createMockInfoCache()

            val cachedInfo = TickerInfoDto(marketValueFormatted = "150.00", deltaFormatted = "+1.00", percentFormatted = "+0.67%", currency = "$")
            coEvery { mockInfoCache.get("AAPL") } returns cachedInfo
            coEvery { mockInfoCache.get("MSFT") } returns cachedInfo

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = emptyMap(),
                infoProviders = mapOf(Provider.FINNHUB to mockInfoProvider),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = mockInfoCache,
                statsService = createMockStatsService(),
            )

            val result = stockProvider.info(listOf("AAPL", "MSFT"))

            Then("should return cached results without calling provider") {
                result.size shouldBe 2
                result.containsKey("AAPL") shouldBe true
                result.containsKey("MSFT") shouldBe true
                coVerify(exactly = 0) { mockInfoProvider.info(any()) }
            }
        }

        When("some tickers are cached, some are not") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockInfoProvider = mockk<StockInfoProvider>()
            val mockInfoCache = createMockInfoCache()

            val cachedInfo = TickerInfoDto(marketValueFormatted = "150.00", deltaFormatted = "+1.00", percentFormatted = "+0.67%", currency = "$")
            val fetchedInfo = TickerInfoDto(marketValueFormatted = "400.00", deltaFormatted = "+2.00", percentFormatted = "+0.50%", currency = "$")

            coEvery { mockInfoCache.get("AAPL") } returns cachedInfo
            coEvery { mockInfoCache.get("MSFT") } returns null

            every { mockProviderConfigService.configs } returns mapOf(Provider.FINNHUB to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.FINNHUB, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockInfoProvider.info(any()) } returns mapOf("MSFT" to fetchedInfo)

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = emptyMap(),
                infoProviders = mapOf(Provider.FINNHUB to mockInfoProvider),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = mockInfoCache,
                statsService = createMockStatsService(),
            )

            val result = stockProvider.info(listOf("AAPL", "MSFT"))

            Then("should return cached and fetched results") {
                result.size shouldBe 2
                result["AAPL"]!!.marketValueFormatted shouldBe "150.00"
                result["MSFT"]!!.marketValueFormatted shouldBe "400.00"
            }
        }
    }

    Given("StockProvider provider priority selection") {

        When("multiple providers available, selects highest priority") {
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
            coEvery { mockLimitUsageService.getUsage(Provider.FINNHUB) } returns LimitUsage(usedCount = 60)

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
    }

    Given("StockProvider stats recording") {

        When("provider call succeeds") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockStatsService = createMockStatsService()

            every { mockProviderConfigService.configs } returns mapOf(Provider.ALPHAVANTAGE to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockSearchProvider.search("AAPL") } returns listOf(TickerDto(ticker = "AAPL", company = "Apple Inc"))

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = mockStatsService,
            )

            stockProvider.search("AAPL")

            Then("should record selection") {
                coVerify(exactly = 1) { mockStatsService.recordSelection(Provider.ALPHAVANTAGE) }
            }

            Then("should not record failure") {
                coVerify(exactly = 0) { mockStatsService.recordFailure(any()) }
            }
        }

        When("provider call fails") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockStatsService = createMockStatsService()

            every { mockProviderConfigService.configs } returns mapOf(Provider.ALPHAVANTAGE to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockSearchProvider.search("AAPL") } throws RuntimeException("API Error")

            val stockProvider = StockProvider(
                providerConfigService = mockProviderConfigService,
                limitUsageService = mockLimitUsageService,
                newsProviders = emptyMap(),
                searchProviders = mapOf(Provider.ALPHAVANTAGE to mockSearchProvider),
                infoProviders = emptyMap(),
                searchCache = createMockSearchCache(),
                newsCache = createMockNewsCache(),
                infoCache = createMockInfoCache(),
                statsService = mockStatsService,
            )

            Then("should record both selection and failure") {
                shouldThrow<RuntimeException> {
                    stockProvider.search("AAPL")
                }
                coVerify(exactly = 1) { mockStatsService.recordSelection(Provider.ALPHAVANTAGE) }
                coVerify(exactly = 1) { mockStatsService.recordFailure(Provider.ALPHAVANTAGE) }
            }
        }
    }

    Given("StockProvider empty results handling") {

        When("search returns empty list") {
            val mockProviderConfigService = mockk<ProviderConfigService>()
            val mockLimitUsageService = mockk<FirestoreLimitUsageService>()
            val mockSearchProvider = mockk<StockSearchProvider>()
            val mockSearchCache = createMockSearchCache()

            every { mockProviderConfigService.configs } returns mapOf(Provider.ALPHAVANTAGE to testConfig)
            coEvery { mockLimitUsageService.getUsage(Provider.ALPHAVANTAGE) } returns LimitUsage(usedCount = 0)
            coEvery { mockLimitUsageService.tryIncrementUsage(Provider.ALPHAVANTAGE, testConfig.limit) } returns LimitUsage(usedCount = 1)
            coEvery { mockSearchProvider.search("UNKNOWN") } returns emptyList()

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

            val result = stockProvider.search("UNKNOWN")

            Then("should return empty list without caching") {
                result shouldBe emptyList()
                coVerify(exactly = 0) { mockSearchCache.put(any(), any()) }
            }
        }
    }
})
