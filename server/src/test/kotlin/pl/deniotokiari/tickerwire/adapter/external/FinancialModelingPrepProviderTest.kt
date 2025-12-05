package pl.deniotokiari.tickerwire.adapter.external

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
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

class FinancialModelingPrepProviderTest : BehaviorSpec({

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
        every { mockProviderConfigService.get(Provider.FINANCIALMODELINGPREP) } returns ProviderConfig(
            apiUri = "https://financialmodelingprep.com/stable",
            apiKey = "test_api_key",
            limit = LimitConfig(perDay = 250),
        )
    }

    Given("FinancialModelIngPrepProvider search") {

        When("searching for a valid query") {
            val responseJson = """
                [
                    {"symbol": "AAPL", "name": "Apple Inc.", "currency": "USD", "exchangeShortName": "NASDAQ"},
                    {"symbol": "AAPL.MX", "name": "Apple Inc.", "currency": "MXN", "exchangeShortName": "BMV"}
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should return list of tickers") {
                val result = provider.search("AAPL")

                result shouldHaveSize 2
                result[0].ticker shouldBe "AAPL"
                result[0].company shouldBe "Apple Inc."
                result[1].ticker shouldBe "AAPL.MX"
            }
        }

        When("search returns empty results") {
            val responseJson = "[]"

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should return empty list") {
                val result = provider.search("NONEXISTENT")
                result.shouldBeEmpty()
            }
        }

        When("search returns results with null fields") {
            val responseJson = """
                [
                    {"symbol": "AAPL", "name": "Apple Inc."},
                    {"symbol": null, "name": "Missing Symbol"},
                    {"symbol": "MSFT", "name": null}
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should filter out results with null required fields") {
                val result = provider.search("test")

                result shouldHaveSize 1
                result[0].ticker shouldBe "AAPL"
            }
        }
    }

    Given("FinancialModelIngPrepProvider info") {

        When("fetching info for valid tickers") {
            val responseJson = """
                [
                    {
                        "symbol": "AAPL",
                        "name": "Apple Inc.",
                        "price": 185.92,
                        "change": 2.45,
                        "changesPercentage": 1.34
                    }
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

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
                [
                    {
                        "symbol": "TSLA",
                        "price": 250.00,
                        "change": -5.75,
                        "changesPercentage": -2.25
                    }
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should format negative values correctly") {
                val result = provider.info(listOf("TSLA"))

                result["TSLA"]!!.marketValueFormatted shouldBe "250.00"
                result["TSLA"]!!.deltaFormatted shouldBe "-5.75"
                result["TSLA"]!!.percentFormatted shouldBe "-2.25%"
            }
        }

        When("fetching info with empty ticker list") {
            val client = createMockClient("[]")
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should return empty map") {
                val result = provider.info(emptyList())
                result.shouldBeEmpty()
            }
        }

        When("fetching info for multiple tickers") {
            val responseJson = """
                [
                    {"symbol": "AAPL", "price": 185.92, "change": 2.45, "changesPercentage": 1.34},
                    {"symbol": "MSFT", "price": 378.91, "change": -1.23, "changesPercentage": -0.32}
                ]
            """.trimIndent()

            val client = createMockClient(responseJson)
            val provider = FinancialModelIngPrepProvider(client, mockProviderConfigService)

            Then("should return info for all tickers") {
                val result = provider.info(listOf("AAPL", "MSFT"))

                result.size shouldBe 2
                result shouldContainKey "AAPL"
                result shouldContainKey "MSFT"
            }
        }
    }
})

