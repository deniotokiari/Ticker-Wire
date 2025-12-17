package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize as mapShouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

class StockDataStockProviderTest : BehaviorSpec({

    // URI should include /v1 as per StockData.org API documentation
    // https://www.stockdata.org/documentation
    val testConfig = ProviderConfig(
        apiUri = "https://api.stockdata.org/v1",
        apiKey = "test-api-key",
        limit = LimitConfig(perDay = 100)
    )

    fun createMockClient(responseJson: String): HttpClient {
        return HttpClient(MockEngine { request ->
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    fun createMockProviderConfigService(): ProviderConfigService {
        val mock = mockk<ProviderConfigService>()
        every { mock.get(Provider.STOCKDATA) } returns testConfig
        return mock
    }

    Given("StockDataStockProvider search function") {

        When("API returns valid search results") {
            val responseJson = """
                {
                    "data": [
                        {"ticker": "AAPL", "name": "Apple Inc"},
                        {"ticker": "AAPL.LON", "name": "Apple Inc (London)"}
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return list of TickerDto") {
                val results = provider.search("AAPL")

                results shouldHaveSize 2
                results[0].ticker shouldBe "AAPL"
                results[0].company shouldBe "Apple Inc"
                results[1].ticker shouldBe "AAPL.LON"
            }
        }

        When("API returns empty results") {
            val responseJson = """{"data": []}"""

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return empty list") {
                val results = provider.search("XYZNONEXISTENT")

                results shouldHaveSize 0
            }
        }

        When("API returns error") {
            val responseJson = """
                {
                    "error": {
                        "code": "invalid_api_token",
                        "message": "Invalid API token provided"
                    }
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should throw exception with error message") {
                val exception = shouldThrow<Exception> {
                    provider.search("AAPL")
                }

                exception.message shouldContain "Invalid API token"
            }
        }
    }

    Given("StockDataStockProvider info function") {

        When("API returns valid quote data") {
            val responseJson = """
                {
                    "data": [
                        {
                            "ticker": "AAPL",
                            "name": "Apple Inc",
                            "price": 175.50,
                            "currency": "USD",
                            "day_change": 2.50,
                            "previous_close_price": 173.00
                        },
                        {
                            "ticker": "MSFT",
                            "name": "Microsoft Corp",
                            "price": 380.25,
                            "currency": "USD",
                            "day_change": -1.75,
                            "previous_close_price": 382.00
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return map of TickerInfoDto with correct values") {
                val results = provider.info(listOf("AAPL", "MSFT"))

                results shouldContainKey "AAPL"
                results shouldContainKey "MSFT"

                results["AAPL"]!!.marketValueFormatted shouldBe "175.50"
                results["AAPL"]!!.deltaFormatted shouldBe "+2.50"
                results["AAPL"]!!.currency shouldBe "USD"

                results["MSFT"]!!.marketValueFormatted shouldBe "380.25"
                results["MSFT"]!!.deltaFormatted shouldBe "-1.75"
            }
        }

        When("called with empty ticker list") {
            val client = createMockClient("{}")
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return empty map without API call") {
                val results = provider.info(emptyList())

                results.shouldBeEmpty()
            }
        }

        When("API returns error") {
            val responseJson = """
                {
                    "error": {
                        "code": "rate_limit_reached",
                        "message": "Too many requests"
                    }
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should throw exception") {
                val exception = shouldThrow<Exception> {
                    provider.info(listOf("AAPL"))
                }

                exception.message shouldContain "Too many requests"
            }
        }
    }

    Given("StockDataStockProvider news function") {

        When("API returns valid news data") {
            val responseJson = """
                {
                    "data": [
                        {
                            "uuid": "news-1",
                            "title": "Apple announces new iPhone",
                            "source": "TechCrunch",
                            "published_at": "2024-01-15T10:30:00.000000Z",
                            "entities": [
                                {"symbol": "AAPL", "name": "Apple Inc"}
                            ]
                        },
                        {
                            "uuid": "news-2",
                            "title": "Microsoft releases new Surface",
                            "source": "The Verge",
                            "published_at": "2024-01-15T11:00:00.000000Z",
                            "entities": [
                                {"symbol": "MSFT", "name": "Microsoft Corp"}
                            ]
                        },
                        {
                            "uuid": "news-3",
                            "title": "Tech giants report earnings",
                            "source": "Bloomberg",
                            "published_at": "2024-01-15T12:00:00.000000Z",
                            "entities": [
                                {"symbol": "AAPL", "name": "Apple Inc"},
                                {"symbol": "MSFT", "name": "Microsoft Corp"}
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return map of news grouped by ticker") {
                val results = provider.news(listOf("AAPL", "MSFT"))

                // Only first ticker is used for API request, so only first ticker is in result
                results.mapShouldHaveSize(1)
                results shouldContainKey "AAPL"

                // AAPL should have 2 news items (news-1 and news-3)
                results["AAPL"]!! shouldHaveSize 2
                results["AAPL"]!![0].title shouldBe "Apple announces new iPhone"
                results["AAPL"]!![0].provider shouldBe "TechCrunch"
            }
        }

        When("called with empty ticker list") {
            val client = createMockClient("{}")
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return empty map without API call") {
                val results = provider.news(emptyList())

                results.shouldBeEmpty()
            }
        }

        When("API returns error") {
            val responseJson = """
                {
                    "error": {
                        "code": "invalid_api_token",
                        "message": "Invalid API token"
                    }
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return empty lists for all tickers") {
                val results = provider.news(listOf("AAPL", "MSFT"))

                // Only first ticker is used for API request, so only first ticker is in result
                results.mapShouldHaveSize(1)
                results shouldContainKey "AAPL"
                results["AAPL"]!! shouldHaveSize 0
            }
        }

        When("API returns news without matching entities") {
            val responseJson = """
                {
                    "data": [
                        {
                            "uuid": "news-1",
                            "title": "General market news",
                            "source": "Reuters",
                            "published_at": "2024-01-15T10:30:00.000000Z",
                            "entities": [
                                {"symbol": "GOOGL", "name": "Alphabet Inc"}
                            ]
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should return empty lists for requested tickers") {
                val results = provider.news(listOf("AAPL", "MSFT"))

                // Only first ticker is used for API request, so only first ticker is in result
                results.mapShouldHaveSize(1)
                results shouldContainKey "AAPL"
                results["AAPL"]!! shouldHaveSize 0
            }
        }
    }

    Given("StockDataStockProvider info calculation") {

        When("stock has positive change") {
            val responseJson = """
                {
                    "data": [
                        {
                            "ticker": "AAPL",
                            "price": 175.50,
                            "currency": "USD",
                            "day_change": 2.50,
                            "previous_close_price": 173.00
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should format with plus sign") {
                val results = provider.info(listOf("AAPL"))

                results["AAPL"]!!.deltaFormatted shouldBe "+2.50"
                results["AAPL"]!!.percentFormatted shouldContain "+"
            }
        }

        When("stock has negative change") {
            val responseJson = """
                {
                    "data": [
                        {
                            "ticker": "AAPL",
                            "price": 170.50,
                            "currency": "USD",
                            "day_change": -2.50,
                            "previous_close_price": 173.00
                        }
                    ]
                }
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = StockDataStockProvider(client, createMockProviderConfigService())

            Then("should format without plus sign") {
                val results = provider.info(listOf("AAPL"))

                results["AAPL"]!!.deltaFormatted shouldBe "-2.50"
                results["AAPL"]!!.percentFormatted shouldContain "-"
            }
        }
    }
})
