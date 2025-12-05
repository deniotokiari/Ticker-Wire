package pl.deniotokiari.tickerwire.services.analytics

import com.google.api.core.ApiFutures
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.cloud.firestore.QuerySnapshot
import com.google.cloud.firestore.Transaction
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.deniotokiari.tickerwire.models.Provider

class ProviderStatsServiceTest : BehaviorSpec({

    fun createMockFirestore(): Firestore {
        val mockFirestore = mockk<Firestore>()
        val mockCollection = mockk<CollectionReference>()
        val mockDocument = mockk<DocumentReference>()
        val mockSubCollection = mockk<CollectionReference>()
        val mockSubDocument = mockk<DocumentReference>()
        val mockDocSnapshot = mockk<DocumentSnapshot>()
        val mockTransaction = mockk<Transaction>()

        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        every { mockDocument.collection(any()) } returns mockSubCollection
        every { mockSubCollection.document(any()) } returns mockSubDocument
        every { mockSubDocument.get() } returns ApiFutures.immediateFuture(mockDocSnapshot)

        every { mockDocSnapshot.exists() } returns false
        every { mockDocSnapshot.getLong(any()) } returns null

        every { mockFirestore.runTransaction<Any?>(any()) } answers {
            val fn = firstArg<Transaction.Function<Any?>>()
            ApiFutures.immediateFuture(fn.updateCallback(mockTransaction))
        }

        every { mockTransaction.get(any<DocumentReference>()) } returns ApiFutures.immediateFuture(mockDocSnapshot)
        every { mockTransaction.set(any(), any()) } returns mockTransaction
        every { mockTransaction.update(any(), any<Map<String, Any>>()) } returns mockTransaction

        return mockFirestore
    }

    Given("ProviderStatsService") {

        When("recording a selection") {
            val mockFirestore = createMockFirestore()
            val statsService = ProviderStatsService(mockFirestore)

            Then("should write to Firestore") {
                statsService.recordSelection(Provider.FINNHUB)

                verify { mockFirestore.collection("provider_stats") }
            }
        }

        When("recording a failure") {
            val mockFirestore = createMockFirestore()
            val statsService = ProviderStatsService(mockFirestore)

            Then("should write to Firestore") {
                statsService.recordFailure(Provider.FINNHUB)

                verify { mockFirestore.collection("provider_stats") }
            }
        }

        When("getting stats for empty month") {
            val mockFirestore = mockk<Firestore>()
            val mockCollection = mockk<CollectionReference>()
            val mockDocument = mockk<DocumentReference>()
            val mockSubCollection = mockk<CollectionReference>()
            val mockQuerySnapshot = mockk<QuerySnapshot>()

            every { mockFirestore.collection("provider_stats") } returns mockCollection
            every { mockCollection.document(any()) } returns mockDocument
            every { mockDocument.collection("providers") } returns mockSubCollection
            every { mockSubCollection.get() } returns ApiFutures.immediateFuture(mockQuerySnapshot)
            every { mockQuerySnapshot.documents } returns emptyList()

            val statsService = ProviderStatsService(mockFirestore)

            Then("should return empty stats") {
                val stats = statsService.getStatsForMonth("2024-12")

                stats.month shouldBe "2024-12"
                stats.providers.isEmpty() shouldBe true
                stats.totalSelections shouldBe 0
                stats.totalFailures shouldBe 0
            }
        }

        When("getting stats with data") {
            val mockFirestore = mockk<Firestore>()
            val mockCollection = mockk<CollectionReference>()
            val mockDocument = mockk<DocumentReference>()
            val mockSubCollection = mockk<CollectionReference>()
            val mockQuerySnapshot = mockk<QuerySnapshot>()
            val mockProviderDoc = mockk<QueryDocumentSnapshot>()

            every { mockFirestore.collection("provider_stats") } returns mockCollection
            every { mockCollection.document(any()) } returns mockDocument
            every { mockDocument.collection("providers") } returns mockSubCollection
            every { mockSubCollection.get() } returns ApiFutures.immediateFuture(mockQuerySnapshot)

            every { mockQuerySnapshot.documents } returns listOf(mockProviderDoc)
            every { mockProviderDoc.id } returns "FINNHUB"
            every { mockProviderDoc.getLong("selections") } returns 100L
            every { mockProviderDoc.getLong("failures") } returns 5L

            val statsService = ProviderStatsService(mockFirestore)

            Then("should return stats with data") {
                val stats = statsService.getStatsForMonth("2024-12")

                stats.month shouldBe "2024-12"
                stats.providers.size shouldBe 1
                stats.providers["FINNHUB"]!!.selections shouldBe 100
                stats.providers["FINNHUB"]!!.failures shouldBe 5
                stats.totalSelections shouldBe 100
                stats.totalFailures shouldBe 5
            }
        }
    }
})

