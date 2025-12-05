package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig
import pl.deniotokiari.tickerwire.services.ProviderConfigService

class MassiveStockProviderTest : BehaviorSpec({

    val mockProviderConfigService = mockk<ProviderConfigService>()

    fun createMockClient(responseJson: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    beforeSpec {
        every { mockProviderConfigService.get(Provider.MASSIVE) } returns ProviderConfig(
            apiUri = "https://api.massive.com",
            apiKey = "test_api_key",
            limit = LimitConfig(perMinute = 5),
        )
    }

    Given("MassiveStockProvider search") {

        When("searching for a valid query") {
            val responseJson = """
                {
                    "status": "OK",
                    "count": 2,
                    "results": [
                        {"ticker": "AAPL", "name": "Apple Inc.", "market": "stocks", "active": true},
                        {"ticker": "AAPLW", "name": "Apple Inc. Warrants", "market": "stocks", "active": true}
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return list of tickers") {
                val result = provider.search("AAPL")

                result shouldHaveSize 2
                result[0].ticker shouldBe "AAPL"
                result[0].company shouldBe "Apple Inc."
                result[1].ticker shouldBe "AAPLW"
            }
        }

        When("search returns empty results") {
            val responseJson = """{"status": "OK", "count": 0, "results": []}"""

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return empty list") {
                val result = provider.search("NONEXISTENT")
                result.shouldBeEmpty()
            }
        }

        When("search returns results with null fields") {
            val responseJson = """
                {
                    "status": "OK",
                    "count": 3,
                    "results": [
                        {"ticker": "AAPL", "name": "Apple Inc."},
                        {"ticker": null, "name": "Missing Ticker"},
                        {"ticker": "MSFT", "name": null}
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should filter out results with null required fields") {
                val result = provider.search("test")

                result shouldHaveSize 1
                result[0].ticker shouldBe "AAPL"
            }
        }
    }

    Given("MassiveStockProvider news") {

        When("fetching news for valid tickers") {
            val responseJson = """
                {
                    "status": "OK",
                    "count": 2,
                    "results": [
                        {
                            "id": "1",
                            "publisher": {"name": "Reuters", "homepage_url": "https://reuters.com"},
                            "title": "Apple Announces New Product",
                            "published_utc": "2024-01-15T10:30:00Z",
                            "tickers": ["AAPL"]
                        },
                        {
                            "id": "2",
                            "publisher": {"name": "Bloomberg"},
                            "title": "Apple Stock Rises",
                            "published_utc": "2024-01-14T09:00:00Z",
                            "tickers": ["AAPL"]
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return news for each ticker") {
                val result = provider.news(listOf("AAPL"))

                result shouldContainKey "AAPL"
                result["AAPL"] shouldNotBe null
                result["AAPL"]!!.size shouldBe 2
                result["AAPL"]!![0].title shouldBe "Apple Announces New Product"
                result["AAPL"]!![0].provider shouldBe "Reuters"
            }
        }

        When("fetching news with empty ticker list") {
            val client = createMockClient("""{"status": "OK", "results": []}""")
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return empty map") {
                val result = provider.news(emptyList())
                result.shouldBeEmpty()
            }
        }

        When("news item has null title") {
            val responseJson = """
                {
                    "status": "OK",
                    "results": [
                        {
                            "id": "1",
                            "publisher": {"name": "Reuters"},
                            "title": null,
                            "published_utc": "2024-01-15T10:30:00Z"
                        },
                        {
                            "id": "2",
                            "publisher": {"name": "Bloomberg"},
                            "title": "Valid News Item",
                            "published_utc": "2024-01-14T09:00:00Z"
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should filter out news items with null title") {
                val result = provider.news(listOf("AAPL"))

                result["AAPL"]!!.size shouldBe 1
                result["AAPL"]!![0].title shouldBe "Valid News Item"
            }
        }
    }

    Given("MassiveStockProvider info") {

        When("fetching info for valid tickers") {
            val responseJson = """
                {
                    "status": "OK",
                    "ticker": "AAPL",
                    "resultsCount": 1,
                    "results": [
                        {
                            "c": 185.92,
                            "h": 187.50,
                            "l": 183.20,
                            "o": 183.47,
                            "v": 50000000,
                            "t": 1705320000000
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return info for each ticker") {
                val result = provider.info(listOf("AAPL"))

                result shouldContainKey "AAPL"
                result["AAPL"]!!.marketValueFormatted shouldBe "185.92"
                // Change = 185.92 - 183.47 = 2.45
                result["AAPL"]!!.deltaFormatted shouldBe "+2.45"
            }
        }

        When("fetching info with negative change") {
            val responseJson = """
                {
                    "status": "OK",
                    "ticker": "TSLA",
                    "resultsCount": 1,
                    "results": [
                        {
                            "c": 250.00,
                            "o": 255.75
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should format negative values correctly") {
                val result = provider.info(listOf("TSLA"))

                result["TSLA"]!!.marketValueFormatted shouldBe "250.00"
                // Change = 250.00 - 255.75 = -5.75
                result["TSLA"]!!.deltaFormatted shouldBe "-5.75"
            }
        }

        When("fetching info with empty ticker list") {
            val client = createMockClient("""{"status": "OK", "results": []}""")
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should return empty map") {
                val result = provider.info(emptyList())
                result.shouldBeEmpty()
            }
        }

        When("fetching info with null close price") {
            val responseJson = """
                {
                    "status": "OK",
                    "ticker": "AAPL",
                    "resultsCount": 1,
                    "results": [
                        {
                            "c": null,
                            "o": 183.47
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = MassiveStockProvider(client, mockProviderConfigService)

            Then("should not include ticker with null price") {
                val result = provider.info(listOf("AAPL"))
                result.shouldBeEmpty()
            }
        }
    }
})

