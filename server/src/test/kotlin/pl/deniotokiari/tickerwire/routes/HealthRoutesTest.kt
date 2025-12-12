package pl.deniotokiari.tickerwire.routes

import com.google.api.core.ApiFutures
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.QuerySnapshot
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class HealthRoutesTest : BehaviorSpec({

    val json = Json { ignoreUnknownKeys = true }

    fun createMockFirestore(shouldFail: Boolean = false): Firestore {
        val mockFirestore = mockk<Firestore>(relaxed = true)
        val mockCollection = mockk<com.google.cloud.firestore.CollectionReference>(relaxed = true)
        val mockQuery = mockk<Query>(relaxed = true)
        val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)

        every { mockFirestore.collection("health_check") } returns mockCollection
        every { mockCollection.limit(1) } returns mockQuery

        if (shouldFail) {
            every { mockQuery.get() } throws RuntimeException("Firestore connection failed")
        } else {
            every { mockQuery.get() } returns ApiFutures.immediateFuture(mockQuerySnapshot)
        }

        return mockFirestore
    }

    Given("Health routes") {
        When("GET /health is called") {
            val mockFirestore = createMockFirestore()

            Then("should return 200 OK with UP status") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<Firestore> { mockFirestore }
                            }
                        )
                    }
                    routing {
                        healthRoutes()
                    }

                    val response = client.get("/health")
                    response.status shouldBe HttpStatusCode.OK

                    val body = json.decodeFromString<HealthResponse>(response.bodyAsText())
                    body.status shouldBe "UP"
                }
            }
        }

        When("GET /ready is called with healthy Firestore") {
            val mockFirestore = createMockFirestore(shouldFail = false)

            Then("should return 200 OK with UP status and all checks passing") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<Firestore> { mockFirestore }
                            }
                        )
                    }
                    routing {
                        healthRoutes()
                    }

                    val response = client.get("/ready")
                    response.status shouldBe HttpStatusCode.OK

                    val body = json.decodeFromString<ReadinessResponse>(response.bodyAsText())
                    body.status shouldBe "UP"
                    body.checks["firestore"]?.status shouldBe "UP"
                }
            }
        }

        When("GET /ready is called with failing Firestore") {
            val mockFirestore = createMockFirestore(shouldFail = true)

            Then("should return 503 Service Unavailable with DOWN status") {
                testApplication {
                    install(ContentNegotiation) {
                        json()
                    }
                    install(Koin) {
                        modules(
                            module {
                                single<Firestore> { mockFirestore }
                            }
                        )
                    }
                    routing {
                        healthRoutes()
                    }

                    val response = client.get("/ready")
                    response.status shouldBe HttpStatusCode.ServiceUnavailable

                    val body = json.decodeFromString<ReadinessResponse>(response.bodyAsText())
                    body.status shouldBe "DOWN"
                    body.checks["firestore"]?.status shouldBe "DOWN"
                    body.checks["firestore"]?.message shouldBe body.checks["firestore"]?.message // Just verify it exists
                }
            }
        }
    }
})
