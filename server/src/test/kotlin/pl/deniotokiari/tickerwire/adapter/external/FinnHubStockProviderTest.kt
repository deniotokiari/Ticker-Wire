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

class FinnHubStockProviderTest : BehaviorSpec({

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
        every { mockProviderConfigService.get(Provider.FINNHUB) } returns ProviderConfig(
            apiUri = "https://finnhub.io/api/v1",
            apiKey = "test_api_key",
            limit = LimitConfig(perMinute = 60),
        )
    }

    Given("FinnHubStockProvider search") {

        When("searching for a valid query") {
            val responseJson = """
                {
                    "count": 2,
                    "result": [
                        {"description": "APPLE INC", "displaySymbol": "AAPL", "symbol": "AAPL", "type": "Common Stock"},
                        {"description": "APPLE INC", "displaySymbol": "AAPL.MX", "symbol": "AAPL.MX", "type": "Common Stock"}
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should return list of tickers") {
                val result = provider.search("AAPL")

                result shouldHaveSize 2
                result[0].ticker shouldBe "AAPL"
                result[0].company shouldBe "APPLE INC"
                result[1].ticker shouldBe "AAPL.MX"
            }
        }

        When("search returns empty results") {
            val responseJson = """{"count": 0, "result": []}"""

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should return empty list") {
                val result = provider.search("NONEXISTENT")
                result.shouldBeEmpty()
            }
        }

        When("search returns results with null fields") {
            val responseJson = """
                {
                    "count": 3,
                    "result": [
                        {"description": "APPLE INC", "symbol": "AAPL"},
                        {"description": null, "symbol": "MISSING"},
                        {"description": "MICROSOFT", "symbol": null}
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should filter out results with null required fields") {
                val result = provider.search("test")

                result shouldHaveSize 1
                result[0].ticker shouldBe "AAPL"
            }
        }
    }

    Given("FinnHubStockProvider news") {

        When("fetching news for valid tickers") {
            val responseJson = """
                [
                    {
                        "category": "company",
                        "datetime": 1705320000,
                        "headline": "Apple Announces New Product",
                        "id": 12345,
                        "source": "Reuters",
                        "summary": "Apple unveiled...",
                        "url": "https://example.com/news1"
                    },
                    {
                        "category": "company",
                        "datetime": 1705233600,
                        "headline": "Apple Stock Rises",
                        "id": 12346,
                        "source": "Bloomberg",
                        "summary": "Apple shares...",
                        "url": "https://example.com/news2"
                    }
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

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
            val client = createMockClient("[]")
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should return empty map") {
                val result = provider.news(emptyList())
                result.shouldBeEmpty()
            }
        }

        When("news item has null headline") {
            val responseJson = """
                [
                    {
                        "datetime": 1705320000,
                        "headline": null,
                        "source": "Reuters"
                    },
                    {
                        "datetime": 1705233600,
                        "headline": "Valid News Item",
                        "source": "Bloomberg"
                    }
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should filter out news items with null headline") {
                val result = provider.news(listOf("AAPL"))

                result["AAPL"]!!.size shouldBe 1
                result["AAPL"]!![0].title shouldBe "Valid News Item"
            }
        }
    }

    Given("FinnHubStockProvider info") {

        When("fetching info for valid tickers") {
            val responseJson = """
                {
                    "c": 185.92,
                    "d": 2.45,
                    "dp": 1.34,
                    "h": 187.50,
                    "l": 183.20,
                    "o": 184.00,
                    "pc": 183.47,
                    "t": 1705320000
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should return info for each ticker") {
                val result = provider.info(listOf("AAPL"))

                result shouldContainKey "AAPL"
                result["AAPL"]!!.marketValueFormatted shouldBe "185.92"
                result["AAPL"]!!.deltaFormatted shouldBe "+2.45"
                result["AAPL"]!!.percentFormatted shouldBe "+1.34%"
            }
        }

        When("fetching info with negative change") {
            val responseJson = """
                {
                    "c": 250.00,
                    "d": -5.75,
                    "dp": -2.25,
                    "h": 255.00,
                    "l": 248.00,
                    "o": 253.00,
                    "pc": 255.75
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should format negative values correctly") {
                val result = provider.info(listOf("TSLA"))

                result["TSLA"]!!.marketValueFormatted shouldBe "250.00"
                result["TSLA"]!!.deltaFormatted shouldBe "-5.75"
                result["TSLA"]!!.percentFormatted shouldBe "-2.25%"
            }
        }

        When("fetching info with empty ticker list") {
            val client = createMockClient("{}")
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should return empty map") {
                val result = provider.info(emptyList())
                result.shouldBeEmpty()
            }
        }

        When("fetching info with null current price") {
            val responseJson = """
                {
                    "c": null,
                    "d": 2.45,
                    "dp": 1.34
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinnHubStockProvider(client, mockProviderConfigService)

            Then("should not include ticker with null price") {
                val result = provider.info(listOf("AAPL"))
                result.shouldBeEmpty()
            }
        }
    }
})

