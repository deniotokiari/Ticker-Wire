package pl.deniotokiari.tickerwire.services.analytics

import com.google.api.core.ApiFutures
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
import com.google.cloud.firestore.WriteResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.deniotokiari.tickerwire.models.Provider

class FirebaseAnalyticsServiceTest : BehaviorSpec({

    fun createMockFirestore(): Firestore {
        val mockFirestore = mockk<Firestore>()
        val mockCollection = mockk<CollectionReference>()
        val mockDocument = mockk<DocumentReference>()
        val mockSubCollection = mockk<CollectionReference>()
        val mockSubDocument = mockk<DocumentReference>()
        val mockWriteResult = mockk<WriteResult>()
        val mockDocSnapshot = mockk<DocumentSnapshot>()
        val mockTransaction = mockk<Transaction>()

        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument
        every { mockDocument.collection(any()) } returns mockSubCollection
        every { mockSubCollection.document(any()) } returns mockSubDocument
        every { mockSubCollection.add(any()) } returns ApiFutures.immediateFuture(mockDocument)
        every { mockSubDocument.get() } returns ApiFutures.immediateFuture(mockDocSnapshot)

        every { mockDocSnapshot.exists() } returns false
        every { mockDocSnapshot.data } returns null

        every { mockFirestore.runTransaction<Any?>(any()) } answers {
            val fn = firstArg<Transaction.Function<Any?>>()
            ApiFutures.immediateFuture(fn.updateCallback(mockTransaction))
        }

        every { mockTransaction.get(any<DocumentReference>()) } returns ApiFutures.immediateFuture(mockDocSnapshot)
        every { mockTransaction.set(any(), any()) } returns mockTransaction
        every { mockTransaction.update(any(), any<Map<String, Any>>()) } returns mockTransaction

        return mockFirestore
    }

    Given("FirebaseAnalyticsService") {

        When("tracking provider selection") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            analytics.trackProviderSelected(Provider.FINNHUB, OperationType.SEARCH)

            Then("should log event to Firestore") {
                // Give async operation time to complete
                Thread.sleep(100)

                verify(atLeast = 1) { mockFirestore.collection("provider_analytics") }
            }
        }

        When("tracking provider failure") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            analytics.trackProviderFailed(Provider.FINNHUB, OperationType.SEARCH, "Connection timeout")

            Then("should log failure event") {
                Thread.sleep(100)
                verify(atLeast = 1) { mockFirestore.collection("provider_analytics") }
            }
        }

        When("tracking cache hit") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            analytics.trackCacheHit(OperationType.SEARCH)

            Then("should log cache hit event") {
                Thread.sleep(100)
                verify(atLeast = 1) { mockFirestore.collection("provider_analytics") }
            }
        }

        When("tracking cache miss") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            analytics.trackCacheMiss(OperationType.NEWS)

            Then("should log cache miss event") {
                Thread.sleep(100)
                verify(atLeast = 1) { mockFirestore.collection("provider_analytics") }
            }
        }

        When("tracking request completion") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            analytics.trackRequestCompleted(OperationType.INFO, 150L, Provider.MASSIVE)

            Then("should log completion event with latency") {
                Thread.sleep(100)
                verify(atLeast = 1) { mockFirestore.collection("provider_analytics") }
            }
        }

        When("getting daily summary with no data") {
            val mockFirestore = createMockFirestore()
            val analytics = FirebaseAnalyticsService(mockFirestore)

            Then("should return null") {
                val summary = analytics.getDailySummary()
                summary shouldBe null
            }
        }
    }
})

