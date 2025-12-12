package pl.deniotokiari.tickerwire.services

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutures
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.WriteBatch
import com.google.cloud.firestore.WriteResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import pl.deniotokiari.tickerwire.models.Provider
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FirestoreLimitUsageServiceTest : BehaviorSpec({

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun createMockFirestore(): Firestore {
        return mockk<Firestore>(relaxed = true)
    }

    fun createMockDocument(exists: Boolean, lastUsed: String? = null, usedCount: Long? = null): DocumentSnapshot {
        val doc = mockk<DocumentSnapshot>(relaxed = true)
        every { doc.exists() } returns exists
        every { doc.getString("last_used") } returns lastUsed
        every { doc.getLong("used_count") } returns usedCount
        return doc
    }

    Given("FirestoreLimitUsageService") {
        val mockFirestore = createMockFirestore()
        val service = FirestoreLimitUsageService(mockFirestore)
        val mockCollection = mockk<com.google.cloud.firestore.CollectionReference>(relaxed = true)
        val mockDocRef = mockk<DocumentReference>(relaxed = true)

        every { mockFirestore.collection("provider_limits") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocRef

        When("getting usage for a provider that doesn't exist") {
            val mockDoc = createMockDocument(exists = false)
            every { mockDocRef.get() } returns ApiFutures.immediateFuture(mockDoc)

            Then("should return empty LimitUsage") {
                runBlocking {
                    val usage = service.getUsage(Provider.ALPHAVANTAGE)
                    usage.usedCount shouldBe 0
                    usage.lastUsed shouldBe null
                }
            }
        }

        When("getting usage for a provider that exists") {
            val now = LocalDateTime.now()
            val mockDoc = createMockDocument(
                exists = true,
                lastUsed = now.format(formatter),
                usedCount = 5L
            )
            every { mockDocRef.get() } returns ApiFutures.immediateFuture(mockDoc)

            Then("should return LimitUsage with correct values") {
                runBlocking {
                    val usage = service.getUsage(Provider.ALPHAVANTAGE)
                    usage.usedCount shouldBe 5
                    usage.lastUsed shouldNotBe null
                }
            }
        }

        // Note: Transaction-based methods (tryIncrementUsage, incrementUsage) are complex to test
        // due to Firestore transaction API. These are tested indirectly through integration tests
        // or through the StockProvider tests that use these services.

        When("resetUsage is called") {
            val mockDeleteFuture = mockk<ApiFuture<WriteResult>>(relaxed = true)
            every { mockDocRef.delete() } returns mockDeleteFuture
            every { mockDeleteFuture.get() } returns mockk()

            Then("should delete the document") {
                runBlocking {
                    service.resetUsage(Provider.ALPHAVANTAGE)
                    verify { mockDocRef.delete() }
                }
            }
        }

        When("resetAllUsage is called") {
            val mockDoc1 = mockk<DocumentReference>(relaxed = true)
            val mockDoc2 = mockk<DocumentReference>(relaxed = true)
            val mockBatch = mockk<WriteBatch>(relaxed = true)
            val mockCommitFuture = mockk<ApiFuture<List<WriteResult>>>(relaxed = true)

            every { mockCollection.listDocuments() } returns listOf(mockDoc1, mockDoc2)
            every { mockFirestore.batch() } returns mockBatch
            every { mockBatch.delete(any()) } returns mockBatch
            every { mockBatch.commit() } returns mockCommitFuture
            every { mockCommitFuture.get() } returns emptyList()

            Then("should delete all documents") {
                runBlocking {
                    service.resetAllUsage()
                    verify { mockBatch.delete(mockDoc1) }
                    verify { mockBatch.delete(mockDoc2) }
                    verify { mockBatch.commit() }
                }
            }
        }

        When("getAllUsage is called") {
            val mockQuerySnapshot = mockk<com.google.cloud.firestore.QuerySnapshot>(relaxed = true)
            val mockDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
            val mockDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)

            every { mockDoc1.id } returns "ALPHAVANTAGE"
            every { mockDoc1.getString("last_used") } returns LocalDateTime.now().format(formatter)
            every { mockDoc1.getLong("used_count") } returns 10L

            every { mockDoc2.id } returns "FINNHUB"
            every { mockDoc2.getString("last_used") } returns LocalDateTime.now().format(formatter)
            every { mockDoc2.getLong("used_count") } returns 20L

            every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)
            every { mockCollection.get() } returns ApiFutures.immediateFuture(mockQuerySnapshot)

            Then("should return map of all providers") {
                runBlocking {
                    val result = service.getAllUsage()
                    result.size shouldBe 2
                    result[Provider.ALPHAVANTAGE]?.usedCount shouldBe 10
                    result[Provider.FINNHUB]?.usedCount shouldBe 20
                }
            }
        }

        When("getAllUsage encounters invalid provider name") {
            val mockQuerySnapshot = mockk<com.google.cloud.firestore.QuerySnapshot>(relaxed = true)
            val mockDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
            val mockDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)

            every { mockDoc1.id } returns "ALPHAVANTAGE"
            every { mockDoc1.getString("last_used") } returns LocalDateTime.now().format(formatter)
            every { mockDoc1.getLong("used_count") } returns 10L

            every { mockDoc2.id } returns "INVALID_PROVIDER"
            every { mockDoc2.getString("last_used") } returns LocalDateTime.now().format(formatter)
            every { mockDoc2.getLong("used_count") } returns 20L

            every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)
            every { mockCollection.get() } returns ApiFutures.immediateFuture(mockQuerySnapshot)

            Then("should skip invalid providers and return valid ones") {
                runBlocking {
                    val result = service.getAllUsage()
                    result.size shouldBe 1
                    result[Provider.ALPHAVANTAGE]?.usedCount shouldBe 10
                }
            }
        }
    }
})

