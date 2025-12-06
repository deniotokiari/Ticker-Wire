package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
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

class AlphaVantageStockProviderTest : BehaviorSpec({

    val mockProviderConfigService = mockk<ProviderConfigService>()
    val testConfig = ProviderConfig(
        apiUri = "https://www.alphavantage.co/query",
        apiKey = "test-api-key",
        limit = LimitConfig(perDay = 25)
    )

    beforeSpec {
        every { mockProviderConfigService.get(Provider.ALPHAVANTAGE) } returns testConfig
    }

    // ============ SEARCH TESTS ============

    Given("AlphaVantageStockProvider search function") {

        When("searching for 'AAPL' with valid response") {
            val mockEngine = MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("function=SYMBOL_SEARCH")) {
                    respond(
                        content = """
                            {
                                "bestMatches": [
                                    {
                                        "1. symbol": "AAPL",
                                        "2. name": "Apple Inc"
                                    },
                                    {
                                        "1. symbol": "AAPL.LON",
                                        "2. name": "Apple Inc - London"
                                    }
                                ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                } else {
                    respond("Not Found", HttpStatusCode.NotFound)
                }
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.search("AAPL")

            Then("should return 2 tickers") {
                results shouldHaveSize 2
            }

            Then("first result should be AAPL") {
                results[0].ticker shouldBe "AAPL"
                results[0].company shouldBe "Apple Inc"
            }

            Then("second result should be AAPL.LON") {
                results[1].ticker shouldBe "AAPL.LON"
                results[1].company shouldBe "Apple Inc - London"
            }
        }

        When("searching with empty response") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{ "bestMatches": [] }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.search("NONEXISTENT")

            Then("should return empty list") {
                results shouldHaveSize 0
            }
        }

        When("API returns an error message") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{ "Error Message": "Invalid API call" }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)

            Then("should throw exception with error message") {
                val exception = runCatching { provider.search("AAPL") }.exceptionOrNull()
                exception?.message shouldContain "Invalid API call"
            }
        }

        When("API returns a rate limit note") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{ "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute" }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)

            Then("should throw exception with note") {
                val exception = runCatching { provider.search("AAPL") }.exceptionOrNull()
                exception?.message shouldContain "Alpha Vantage API note"
            }
        }
    }

    // ============ NEWS TESTS ============

    Given("AlphaVantageStockProvider news function") {

        When("getting news for a ticker with valid response") {
            val mockEngine = MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("function=NEWS_SENTIMENT")) {
                    respond(
                        content = """
                            {
                                "feed": [
                                    {
                                        "title": "Apple announces new iPhone",
                                        "time_published": "20240115T120000",
                                        "source": "TechNews",
                                        "ticker_sentiment": [
                                            { "ticker": "AAPL" }
                                        ]
                                    },
                                    {
                                        "title": "Apple stock rises",
                                        "time_published": "20240115T110000",
                                        "source": "FinanceNews",
                                        "ticker_sentiment": [
                                            { "ticker": "AAPL" }
                                        ]
                                    }
                                ]
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                } else {
                    respond("Not Found", HttpStatusCode.NotFound)
                }
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.news(listOf("AAPL"))

            Then("should return news for AAPL") {
                results.containsKey("AAPL") shouldBe true
                results["AAPL"]!! shouldHaveSize 2
            }

            Then("news items should have correct data") {
                val news = results["AAPL"]!!
                news[0].title shouldBe "Apple announces new iPhone"
                news[0].provider shouldBe "TechNews"
                news[0].dateTimeFormatted shouldBe "2024-01-15 12:00:00"
            }
        }

        When("getting news for empty ticker list") {
            val mockEngine = MockEngine { _ ->
                respond("Should not be called", HttpStatusCode.InternalServerError)
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.news(emptyList())

            Then("should return empty map without calling API") {
                results shouldBe emptyMap()
            }
        }

        When("API returns error for news") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{ "Error Message": "Invalid ticker" }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.news(listOf("INVALID"))

            Then("should return empty list for that ticker") {
                results["INVALID"] shouldBe emptyList()
            }
        }

        When("news feed has items without matching ticker sentiment") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "feed": [
                                {
                                    "title": "General market news",
                                    "time_published": "20240115T120000",
                                    "source": "MarketNews",
                                    "ticker_sentiment": [
                                        { "ticker": "MSFT" }
                                    ]
                                }
                            ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.news(listOf("AAPL"))

            Then("should return empty list (no matching ticker sentiment)") {
                results["AAPL"] shouldBe emptyList()
            }
        }
    }

    // ============ INFO TESTS ============

    Given("AlphaVantageStockProvider info function") {

        When("getting info for a ticker with valid response") {
            val mockEngine = MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("function=GLOBAL_QUOTE")) {
                    respond(
                        content = """
                            {
                                "Global Quote": {
                                    "05. price": "152.5000",
                                    "09. change": "2.50",
                                    "10. change percent": "1.67%"
                                }
                            }
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                } else {
                    respond("Not Found", HttpStatusCode.NotFound)
                }
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("AAPL"))

            Then("should return info for AAPL") {
                results.containsKey("AAPL") shouldBe true
            }

            Then("info should have correct formatted values") {
                val info = results["AAPL"]!!
                info.marketValueFormatted shouldBe "152.50"
                info.deltaFormatted shouldBe "+2.50"
                info.percentFormatted shouldBe "+1.67%"
            }
        }

        When("getting info with negative change") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "Global Quote": {
                                "05. price": "148.0000",
                                "09. change": "-4.50",
                                "10. change percent": "-2.95%"
                            }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("TSLA"))

            Then("should show negative change correctly") {
                val info = results["TSLA"]!!
                info.deltaFormatted shouldBe "-4.50"
                info.percentFormatted shouldBe "-2.95%"
            }
        }

        When("getting info for empty ticker list") {
            val mockEngine = MockEngine { _ ->
                respond("Should not be called", HttpStatusCode.InternalServerError)
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.info(emptyList())

            Then("should return empty map without calling API") {
                results shouldBe emptyMap()
            }
        }

        When("API returns error for info") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{ "Error Message": "Invalid symbol" }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("INVALID"))

            Then("should return empty map") {
                results shouldBe emptyMap()
            }
        }

        When("getting info for multiple tickers (per-ticker provider only processes first)") {
            var callCount = 0
            val mockEngine = MockEngine { request ->
                callCount++
                val url = request.url.toString()
                val symbol = if (url.contains("symbol=AAPL")) "AAPL" else "MSFT"
                val price = if (symbol == "AAPL") "150.0000" else "400.0000"

                respond(
                    content = """
                        {
                            "Global Quote": {
                                "05. price": "$price",
                                "09. change": "1.00",
                                "10. change percent": "0.50%"
                            }
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = AlphaVantageStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("AAPL", "MSFT"))

            Then("should make only one API call (per-ticker provider handles one at a time)") {
                callCount shouldBe 1
            }

            Then("should return info for only the first ticker") {
                results.size shouldBe 1
                results.containsKey("AAPL") shouldBe true
                results.containsKey("MSFT") shouldBe false
            }
        }
    }
})

