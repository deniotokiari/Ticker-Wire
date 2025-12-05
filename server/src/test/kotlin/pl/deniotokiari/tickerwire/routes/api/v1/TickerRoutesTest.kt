package pl.deniotokiari.tickerwire.routes.api.v1

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import pl.deniotokiari.tickerwire.adapter.StockProvider
import pl.deniotokiari.tickerwire.model.dto.TickerDto
import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import pl.deniotokiari.tickerwire.model.dto.TickerNewsDto
import pl.deniotokiari.tickerwire.models.ErrorResponse

class TickerRoutesTest : BehaviorSpec({

    val json = Json { ignoreUnknownKeys = true }

    Given("GET /api/v1/tickers/search endpoint") {

        When("searching with valid query") {
            val mockStockProvider = mockk<StockProvider>()

            coEvery { mockStockProvider.search("AAPL") } returns listOf(
                TickerDto(ticker = "AAPL", company = "Apple Inc"),
                TickerDto(ticker = "AAPL.LON", company = "Apple Inc - London")
            )

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.get("/api/v1/tickers/search?query=AAPL")

                Then("should return OK status") {
                    response.status shouldBe HttpStatusCode.OK
                }

                Then("should return search results") {
                    val results = json.decodeFromString<List<TickerDto>>(response.bodyAsText())
                    results.size shouldBe 2
                    results[0].ticker shouldBe "AAPL"
                }
            }
        }

        When("searching without query parameter") {
            val mockStockProvider = mockk<StockProvider>()

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.get("/api/v1/tickers/search")

                Then("should return BadRequest or InternalServerError") {
                    // IllegalArgumentException will be thrown
                    response.status shouldBe HttpStatusCode.InternalServerError
                }
            }
        }

        When("searching with empty query") {
            val mockStockProvider = mockk<StockProvider>()

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.get("/api/v1/tickers/search?query=")

                Then("should return error for empty query") {
                    response.status shouldBe HttpStatusCode.InternalServerError
                }
            }
        }

        When("searching with blank query") {
            val mockStockProvider = mockk<StockProvider>()

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.get("/api/v1/tickers/search?query=%20%20%20")

                Then("should return error for blank query") {
                    response.status shouldBe HttpStatusCode.InternalServerError
                }
            }
        }
    }

    Given("POST /api/v1/tickers/news endpoint") {

        When("requesting news with valid tickers") {
            val mockStockProvider = mockk<StockProvider>()

            coEvery { mockStockProvider.news(listOf("AAPL", "MSFT")) } returns mapOf(
                "AAPL" to listOf(
                    TickerNewsDto(
                        title = "Apple News",
                        provider = "TechNews",
                        dateTimeFormatted = "2024-01-15 12:00:00",
                        timestamp = 1705320000000,
                        url = "https://example.com/news/apple",
                    )
                ),
                "MSFT" to listOf(
                    TickerNewsDto(
                        title = "Microsoft News",
                        provider = "FinanceNews",
                        dateTimeFormatted = "2024-01-15 11:00:00",
                        timestamp = 1705316400000,
                        url = "https://example.com/news/msft",
                    )
                )
            )

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.post("/api/v1/tickers/news") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(listOf("AAPL", "MSFT")))
                }

                Then("should return OK status") {
                    response.status shouldBe HttpStatusCode.OK
                }

                Then("should return news for both tickers") {
                    val results = json.decodeFromString<Map<String, List<TickerNewsDto>>>(response.bodyAsText())
                    results.size shouldBe 2
                    results.containsKey("AAPL") shouldBe true
                    results.containsKey("MSFT") shouldBe true
                }
            }
        }

        When("requesting news with empty list") {
            val mockStockProvider = mockk<StockProvider>()

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    install(StatusPages) {
                        exception<RequestValidationException> { call, cause ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    status = HttpStatusCode.BadRequest.value,
                                    message = cause.message ?: "Validation failed",
                                    error = "ValidationError"
                                )
                            )
                        }
                    }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.post("/api/v1/tickers/news") {
                    contentType(ContentType.Application.Json)
                    setBody("[]")
                }

                Then("should return BadRequest for empty list") {
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }

    Given("POST /api/v1/tickers/info endpoint") {

        When("requesting info with valid tickers") {
            val mockStockProvider = mockk<StockProvider>()

            coEvery { mockStockProvider.info(listOf("AAPL", "MSFT")) } returns mapOf(
                "AAPL" to TickerInfoDto(
                    marketValueFormatted = "150.00",
                    deltaFormatted = "+2.00",
                    percentFormatted = "+1.35%",
                    currency = "USD"
                ),
                "MSFT" to TickerInfoDto(
                    marketValueFormatted = "400.00",
                    deltaFormatted = "-5.00",
                    percentFormatted = "-1.23%",
                    currency = "USD"
                )
            )

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.post("/api/v1/tickers/info") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(listOf("AAPL", "MSFT")))
                }

                Then("should return OK status") {
                    response.status shouldBe HttpStatusCode.OK
                }

                Then("should return info for both tickers") {
                    val results = json.decodeFromString<Map<String, TickerInfoDto>>(response.bodyAsText())
                    results.size shouldBe 2
                    results["AAPL"]!!.marketValueFormatted shouldBe "150.00"
                    results["MSFT"]!!.deltaFormatted shouldBe "-5.00"
                }
            }
        }

        When("requesting info for single ticker") {
            val mockStockProvider = mockk<StockProvider>()

            coEvery { mockStockProvider.info(listOf("TSLA")) } returns mapOf(
                "TSLA" to TickerInfoDto(
                    marketValueFormatted = "250.00",
                    deltaFormatted = "+10.00",
                    percentFormatted = "+4.17%",
                    currency = "USD"
                )
            )

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.post("/api/v1/tickers/info") {
                    contentType(ContentType.Application.Json)
                    setBody("""["TSLA"]""")
                }

                Then("should return info for the ticker") {
                    response.status shouldBe HttpStatusCode.OK
                    val results = json.decodeFromString<Map<String, TickerInfoDto>>(response.bodyAsText())
                    results.size shouldBe 1
                    results["TSLA"]!!.percentFormatted shouldBe "+4.17%"
                }
            }
        }

        When("requesting info with empty list") {
            val mockStockProvider = mockk<StockProvider>()

            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    install(StatusPages) {
                        exception<RequestValidationException> { call, cause ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(
                                    status = HttpStatusCode.BadRequest.value,
                                    message = cause.message ?: "Validation failed",
                                    error = "ValidationError"
                                )
                            )
                        }
                    }
                    routing {
                        tickerRoutes(mockStockProvider)
                    }
                }

                val response = client.post("/api/v1/tickers/info") {
                    contentType(ContentType.Application.Json)
                    setBody("[]")
                }

                Then("should return BadRequest for empty list") {
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }
    }
})

