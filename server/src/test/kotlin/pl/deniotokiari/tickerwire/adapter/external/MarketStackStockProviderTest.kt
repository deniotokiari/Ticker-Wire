package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

class MarketStackStockProviderTest : BehaviorSpec({

    val mockProviderConfigService = mockk<ProviderConfigService>()
    val testConfig = ProviderConfig(
        apiUri = "https://api.marketstack.com/v2",
        apiKey = "test-api-key",
        limit = LimitConfig(perDay = 100)
    )

    beforeSpec {
        every { mockProviderConfigService.get(Provider.MARKETSTACK) } returns testConfig
    }

    Given("MarketStackStockProvider search function") {

        When("searching for 'AAPL' with valid response") {
            val mockEngine = MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("/tickerslist")) {
                    respond(
                        content = """
                            {
                                "data": [
                                    {
                                        "ticker": "AAPL",
                                        "name": "Apple Inc"
                                    },
                                    {
                                        "ticker": "AAPL.MX",
                                        "name": "Apple Inc - Mexico"
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

            val provider = MarketStackStockProvider(client, mockProviderConfigService)
            val results = provider.search("AAPL")

            Then("should return 2 tickers") {
                results shouldHaveSize 2
            }

            Then("first result should be AAPL") {
                results[0].ticker shouldBe "AAPL"
                results[0].company shouldBe "Apple Inc"
            }

            Then("second result should be AAPL.MX") {
                results[1].ticker shouldBe "AAPL.MX"
                results[1].company shouldBe "Apple Inc - Mexico"
            }
        }

        When("searching with empty response") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": []
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

            val provider = MarketStackStockProvider(client, mockProviderConfigService)
            val results = provider.search("NONEXISTENT")

            Then("should return empty list") {
                results shouldHaveSize 0
            }
        }

        When("API returns an error") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "error": {
                                "code": "invalid_access_key",
                                "message": "You have not supplied a valid API Access Key."
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

            val provider = MarketStackStockProvider(client, mockProviderConfigService)

            Then("should throw exception with error message") {
                val exception = runCatching { provider.search("AAPL") }.exceptionOrNull()
                exception shouldBe Exception("MarketStack API error: You have not supplied a valid API Access Key.")
            }
        }
    }

    Given("MarketStackStockProvider info function") {

        When("getting info for AAPL and MSFT") {
            val mockEngine = MockEngine { request ->
                val url = request.url.toString()
                if (url.contains("/eod/latest")) {
                    respond(
                        content = """
                            {
                                "data": [
                                    {
                                        "open": 150.00,
                                        "close": 152.50,
                                        "symbol": "AAPL"
                                    },
                                    {
                                        "open": 400.00,
                                        "close": 405.25,
                                        "symbol": "MSFT"
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

            val provider = MarketStackStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("AAPL", "MSFT"))

            Then("should return info for 2 tickers") {
                results.size shouldBe 2
            }

            Then("AAPL info should be correct") {
                val aapl = results["AAPL"]!!
                aapl.marketValueFormatted shouldBe "152.50"
                aapl.deltaFormatted shouldBe "+2.50"
                aapl.percentFormatted shouldBe "+1.67%"
                aapl.currency shouldBe ""
            }

            Then("MSFT info should be correct") {
                val msft = results["MSFT"]!!
                msft.marketValueFormatted shouldBe "405.25"
                msft.deltaFormatted shouldBe "+5.25"
                msft.percentFormatted shouldBe "+1.31%"
                msft.currency shouldBe ""
            }
        }

        When("getting info with negative change") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "open": 160.00,
                                    "close": 150.00,
                                    "symbol": "TSLA"
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

            val provider = MarketStackStockProvider(client, mockProviderConfigService)
            val results = provider.info(listOf("TSLA"))

            Then("should show negative change correctly") {
                val tsla = results["TSLA"]!!
                tsla.marketValueFormatted shouldBe "150.00"
                tsla.deltaFormatted shouldBe "-10.00"
                tsla.percentFormatted shouldBe "-6.25%"
            }
        }

        When("getting info with empty list") {
            val mockEngine = MockEngine { _ ->
                respond("Should not be called", HttpStatusCode.InternalServerError)
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val provider = MarketStackStockProvider(client, mockProviderConfigService)
            val results = provider.info(emptyList())

            Then("should return empty map without calling API") {
                results shouldBe emptyMap()
            }
        }
    }
})

