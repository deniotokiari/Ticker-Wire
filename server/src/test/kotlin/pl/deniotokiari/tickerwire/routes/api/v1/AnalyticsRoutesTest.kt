package pl.deniotokiari.tickerwire.routes.api.v1

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import pl.deniotokiari.tickerwire.services.analytics.MonthlyStats
import pl.deniotokiari.tickerwire.services.analytics.ProviderStat
import pl.deniotokiari.tickerwire.services.analytics.ProviderStatsService

class AnalyticsRoutesTest : BehaviorSpec({

    val json = Json { ignoreUnknownKeys = true }

    fun createMockStatsService(): ProviderStatsService {
        val mockService = mockk<ProviderStatsService>(relaxed = true)
        
        coEvery { mockService.getCurrentMonthStats() } returns MonthlyStats(
            month = "2024-12",
            providers = mapOf(
                "ALPHAVANTAGE" to ProviderStat(provider = "ALPHAVANTAGE", selections = 50, failures = 0),
                "FINNHUB" to ProviderStat(provider = "FINNHUB", selections = 50, failures = 0)
            ),
            totalSelections = 100,
            totalFailures = 0
        )

        coEvery { mockService.getStatsForMonth("2024-11") } returns MonthlyStats(
            month = "2024-11",
            providers = mapOf(
                "ALPHAVANTAGE" to ProviderStat(provider = "ALPHAVANTAGE", selections = 100, failures = 0),
                "FINNHUB" to ProviderStat(provider = "FINNHUB", selections = 100, failures = 0)
            ),
            totalSelections = 200,
            totalFailures = 0
        )

        coEvery { mockService.getStatsForMonth("invalid") } returns MonthlyStats(
            month = "invalid",
            providers = emptyMap(),
            totalSelections = 0,
            totalFailures = 0
        )

        return mockService
    }

    Given("Analytics routes") {
        When("GET /api/v1/stats is called") {
            val mockStatsService = createMockStatsService()

            Then("should return 200 OK with current month stats") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<ProviderStatsService> { mockStatsService }
                            }
                        )
                    }
                    routing {
                        route("/api/v1") {
                            statsRoutes()
                        }
                    }

                    val response = client.get("/api/v1/stats")
                    response.status shouldBe HttpStatusCode.OK

                    val body = json.decodeFromString<MonthlyStats>(response.bodyAsText())
                    body.month shouldBe "2024-12"
                    body.totalSelections shouldBe 100
                }
            }
        }

        When("GET /api/v1/stats/{month} is called with valid month") {
            val mockStatsService = createMockStatsService()

            Then("should return 200 OK with stats for that month") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<ProviderStatsService> { mockStatsService }
                            }
                        )
                    }
                    routing {
                        route("/api/v1") {
                            statsRoutes()
                        }
                    }

                    val response = client.get("/api/v1/stats/2024-11")
                    response.status shouldBe HttpStatusCode.OK

                    val body = json.decodeFromString<MonthlyStats>(response.bodyAsText())
                    body.month shouldBe "2024-11"
                    body.totalSelections shouldBe 200
                }
            }
        }

        When("GET /api/v1/stats/{month} is called with invalid format") {
            val mockStatsService = createMockStatsService()

            Then("should return 400 Bad Request") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<ProviderStatsService> { mockStatsService }
                            }
                        )
                    }
                    routing {
                        route("/api/v1") {
                            statsRoutes()
                        }
                    }

                    val response = client.get("/api/v1/stats/invalid-format")
                    response.status shouldBe HttpStatusCode.BadRequest

                    val body = json.decodeFromString<Map<String, String>>(response.bodyAsText())
                    body["error"] shouldBe "Invalid month format. Use yyyy-MM (e.g., 2024-12)"
                }
            }
        }

        When("GET /api/v1/stats/{month} is called with missing parameter") {
            val mockStatsService = createMockStatsService()

            Then("should return 404 Not Found") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<ProviderStatsService> { mockStatsService }
                            }
                        )
                    }
                    routing {
                        route("/api/v1") {
                            statsRoutes()
                        }
                    }

                    val response = client.get("/api/v1/stats/")
                    response.status shouldBe HttpStatusCode.NotFound
                }
            }
        }
    }
})

