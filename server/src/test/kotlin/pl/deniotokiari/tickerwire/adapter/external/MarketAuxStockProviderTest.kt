package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig
import pl.deniotokiari.tickerwire.services.ProviderConfigService

class MarketAuxStockProviderTest : DescribeSpec({
    // URI should include /v1 as per Marketaux API documentation
    // https://www.marketaux.com/documentation
    val testConfig = ProviderConfig(
        apiUri = "https://api.marketaux.com/v1",
        apiKey = "test-api-key",
        limit = LimitConfig(perDay = 100)
    )

    describe("search") {
        it("should return list of tickers on valid response") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "symbol": "AAPL",
                                    "name": "Apple Inc",
                                    "type": "equity",
                                    "industry": "Technology",
                                    "country": "us"
                                },
                                {
                                    "symbol": "AMZN",
                                    "name": "Amazon.com Inc",
                                    "type": "equity",
                                    "industry": "Consumer Cyclical",
                                    "country": "us"
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.search("apple")

            result shouldHaveSize 2
            result[0].ticker shouldBe "AAPL"
            result[0].company shouldBe "Apple Inc"
            result[1].ticker shouldBe "AMZN"
            result[1].company shouldBe "Amazon.com Inc"
        }

        it("should return empty list when no data found") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{"data": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.search("unknownquery")

            result.shouldBeEmpty()
        }

        it("should throw exception on API error") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "error": {
                                "code": "invalid_api_token",
                                "message": "Invalid API token."
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)

            try {
                provider.search("test")
                throw AssertionError("Expected exception to be thrown")
            } catch (e: Exception) {
                e.message shouldBe "Marketaux API error: Invalid API token."
            }
        }

        it("should filter out entities with null symbol or name") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "symbol": "AAPL",
                                    "name": "Apple Inc",
                                    "type": "equity"
                                },
                                {
                                    "symbol": null,
                                    "name": "Unknown Company",
                                    "type": "equity"
                                },
                                {
                                    "symbol": "MSFT",
                                    "name": null,
                                    "type": "equity"
                                },
                                {
                                    "symbol": "GOOG",
                                    "name": "Alphabet Inc",
                                    "type": "equity"
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.search("tech")

            result shouldHaveSize 2
            result[0].ticker shouldBe "AAPL"
            result[1].ticker shouldBe "GOOG"
        }
    }

    describe("news") {
        it("should return news grouped by ticker") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "uuid": "news-1",
                                    "title": "Apple announces new iPhone",
                                    "source": "TechNews",
                                    "published_at": "2024-01-15T10:30:00.000000Z",
                                    "entities": [
                                        {"symbol": "AAPL", "name": "Apple Inc", "sentiment_score": 0.5}
                                    ]
                                },
                                {
                                    "uuid": "news-2",
                                    "title": "Tesla stock rises",
                                    "source": "MarketWatch",
                                    "published_at": "2024-01-15T11:00:00.000000Z",
                                    "entities": [
                                        {"symbol": "TSLA", "name": "Tesla Inc", "sentiment_score": 0.8}
                                    ]
                                },
                                {
                                    "uuid": "news-3",
                                    "title": "Apple and Tesla partnership",
                                    "source": "Bloomberg",
                                    "published_at": "2024-01-15T12:00:00.000000Z",
                                    "entities": [
                                        {"symbol": "AAPL", "name": "Apple Inc", "sentiment_score": 0.3},
                                        {"symbol": "TSLA", "name": "Tesla Inc", "sentiment_score": 0.4}
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.news(listOf("AAPL", "TSLA"))

            result shouldHaveSize 2
            result["AAPL"]!! shouldHaveSize 2
            result["TSLA"]!! shouldHaveSize 2

            result["AAPL"]!![0].title shouldBe "Apple announces new iPhone"
            result["AAPL"]!![0].provider shouldBe "TechNews"
            result["AAPL"]!![1].title shouldBe "Apple and Tesla partnership"

            result["TSLA"]!![0].title shouldBe "Tesla stock rises"
            result["TSLA"]!![1].title shouldBe "Apple and Tesla partnership"
        }

        it("should return empty map for empty tickers list") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.news(emptyList())

            result shouldHaveSize 0
        }

        it("should return empty lists for tickers without news") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{"data": []}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.news(listOf("UNKNOWN"))

            result shouldHaveSize 1
            result["UNKNOWN"]!!.shouldBeEmpty()
        }

        it("should limit news items to 10 per ticker") {
            val newsItems = (1..15).map { i ->
                """
                    {
                        "uuid": "news-$i",
                        "title": "News $i",
                        "source": "Source",
                        "published_at": "2024-01-15T10:${i.toString().padStart(2, '0')}:00.000000Z",
                        "entities": [{"symbol": "AAPL", "name": "Apple Inc", "sentiment_score": 0.5}]
                    }
                """.trimIndent()
            }

            val mockEngine = MockEngine { _ ->
                respond(
                    content = """{"data": [${newsItems.joinToString(",")}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }

            val client = HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.news(listOf("AAPL"))

            result["AAPL"]!! shouldHaveSize 10
        }

        it("should return empty lists on API error") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "error": {
                                "code": "rate_limit_reached",
                                "message": "Too many requests."
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            val result = provider.news(listOf("AAPL"))

            result shouldHaveSize 1
            result["AAPL"]!!.shouldBeEmpty()
        }

        it("should only include news for requested tickers") {
            val mockEngine = MockEngine { _ ->
                respond(
                    content = """
                        {
                            "data": [
                                {
                                    "uuid": "news-1",
                                    "title": "Tech news",
                                    "source": "TechNews",
                                    "published_at": "2024-01-15T10:30:00.000000Z",
                                    "entities": [
                                        {"symbol": "AAPL", "name": "Apple Inc", "sentiment_score": 0.5},
                                        {"symbol": "GOOG", "name": "Alphabet Inc", "sentiment_score": 0.3}
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

            val mockProviderConfigService = mockk<ProviderConfigService>()
            coEvery { mockProviderConfigService.get(Provider.MARKETAUX) } returns testConfig

            val provider = MarketAuxStockProvider(client, mockProviderConfigService)
            // Only request AAPL, not GOOG
            val result = provider.news(listOf("AAPL"))

            result shouldHaveSize 1
            result["AAPL"]!! shouldHaveSize 1
            result.containsKey("GOOG") shouldBe false
        }
    }
})

