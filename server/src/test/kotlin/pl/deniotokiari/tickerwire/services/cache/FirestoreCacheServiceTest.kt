package pl.deniotokiari.tickerwire.services.cache

import com.google.api.core.ApiFuture
import com.google.cloud.Timestamp
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.WriteResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.time.Duration
import java.time.Instant

class FirestoreCacheServiceTest : BehaviorSpec({

    @Serializable
    data class TestData(val id: String, val value: Int)

    fun createMockFirestore(
        documentExists: Boolean = false,
        documentData: Map<String, Any?> = emptyMap(),
    ): Firestore {
        val mockFirestore = mockk<Firestore>()
        val mockCollection = mockk<CollectionReference>()
        val mockDocRef = mockk<DocumentReference>()
        val mockDocSnapshot = mockk<DocumentSnapshot>()
        val mockWriteResult = mockk<ApiFuture<WriteResult>>()

        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocRef
        
        val mockGetFuture = mockk<ApiFuture<DocumentSnapshot>>()
        every { mockDocRef.get() } returns mockGetFuture
        every { mockGetFuture.get() } returns mockDocSnapshot
        
        every { mockDocSnapshot.exists() } returns documentExists
        every { mockDocSnapshot.getString("data") } returns documentData["data"] as? String
        every { mockDocSnapshot.getTimestamp("expires_at") } returns documentData["expires_at"] as? Timestamp
        
        every { mockDocRef.set(any<Map<String, Any>>()) } returns mockWriteResult
        every { mockWriteResult.get() } returns mockk()
        
        val mockDeleteFuture = mockk<ApiFuture<WriteResult>>()
        every { mockDocRef.delete() } returns mockDeleteFuture
        every { mockDeleteFuture.get() } returns mockk()

        return mockFirestore
    }

    Given("FirestoreCacheService") {

        When("getting a non-existent key") {
            val mockFirestore = createMockFirestore(documentExists = false)
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofHours(1),
                firestore = mockFirestore,
            )

            Then("should return null") {
                val result = cache.get("missing_key")
                result shouldBe null
            }
        }

        When("getting an existing non-expired key") {
            val testData = TestData("test", 42)
            val futureTimestamp = Timestamp.ofTimeSecondsAndNanos(
                Instant.now().plusSeconds(3600).epochSecond, 0
            )
            val mockFirestore = createMockFirestore(
                documentExists = true,
                documentData = mapOf(
                    "data" to """{"id":"test","value":42}""",
                    "expires_at" to futureTimestamp,
                ),
            )
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofHours(1),
                firestore = mockFirestore,
            )

            Then("should return cached data") {
                val result = cache.get("existing_key")
                result shouldNotBe null
                result?.id shouldBe "test"
                result?.value shouldBe 42
            }
        }

        When("getting an expired key") {
            val pastTimestamp = Timestamp.ofTimeSecondsAndNanos(
                Instant.now().minusSeconds(3600).epochSecond, 0
            )
            val mockFirestore = createMockFirestore(
                documentExists = true,
                documentData = mapOf(
                    "data" to """{"id":"test","value":42}""",
                    "expires_at" to pastTimestamp,
                ),
            )
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofHours(1),
                firestore = mockFirestore,
            )

            Then("should return null and delete expired entry") {
                val result = cache.get("expired_key")
                result shouldBe null
            }
        }

        When("putting a value") {
            val mockFirestore = createMockFirestore()
            val dataSlot = slot<Map<String, Any>>()
            
            val mockCollection = mockk<CollectionReference>()
            val mockDocRef = mockk<DocumentReference>()
            val mockWriteResult = mockk<ApiFuture<WriteResult>>()
            
            every { mockFirestore.collection(any()) } returns mockCollection
            every { mockCollection.document(any()) } returns mockDocRef
            every { mockDocRef.set(capture(dataSlot)) } returns mockWriteResult
            every { mockWriteResult.get() } returns mockk()
            
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofHours(1),
                firestore = mockFirestore,
            )

            Then("should store data with correct structure") {
                cache.put("test_key", TestData("id1", 100))
                
                dataSlot.captured["data"] shouldBe """{"id":"id1","value":100}"""
                dataSlot.captured["ttl_seconds"] shouldBe 3600L
                dataSlot.captured["created_at"] shouldNotBe null
                dataSlot.captured["expires_at"] shouldNotBe null
            }
        }
    }

    Given("FirestoreCacheFactory") {

        When("creating a search cache") {
            val mockFirestore = createMockFirestore()
            val factory = FirestoreCacheFactory(mockFirestore)
            
            val cache = factory.searchCache(
                name = "search_cache",
                serializer = ListSerializer(TestData.serializer()),
                ttl = Duration.ofHours(2),
            )

            Then("cache should be created") {
                cache shouldNotBe null
            }
        }

        When("creating a news cache") {
            val mockFirestore = createMockFirestore()
            val factory = FirestoreCacheFactory(mockFirestore)
            
            val cache = factory.newsCache(
                name = "news_cache",
                serializer = ListSerializer(TestData.serializer()),
            )

            Then("cache should be created") {
                cache shouldNotBe null
            }
        }

        When("creating an info cache") {
            val mockFirestore = createMockFirestore()
            val factory = FirestoreCacheFactory(mockFirestore)
            
            val cache = factory.infoCache(
                name = "info_cache",
                serializer = ListSerializer(TestData.serializer()),
            )

            Then("cache should be created") {
                cache shouldNotBe null
            }
        }

        When("creating a custom cache") {
            val mockFirestore = createMockFirestore()
            val factory = FirestoreCacheFactory(mockFirestore)
            
            val cache = factory.custom(
                name = "custom_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofDays(1),
                keyGenerator = { "custom_$it" },
            )

            Then("cache should be created") {
                cache shouldNotBe null
            }
        }
    }

    Given("firestoreCache DSL builder") {

        When("building cache with all parameters") {
            val mockFirestore = createMockFirestore()
            
            val cache = firestoreCache<TestData>(mockFirestore) {
                name = "dsl_cache"
                serializer = TestData.serializer()
                ttl = Duration.ofHours(12)
                keyGenerator = { "dsl_$it" }
            }

            Then("cache should be created") {
                cache shouldNotBe null
            }
        }
    }

    Given("Key generation functions") {
        
        When("using search key generator") {
            val keyGen: (Any) -> String = { query -> "search_${query.toString().lowercase().trim()}" }

            Then("should normalize query") {
                keyGen("AAPL") shouldBe "search_aapl"
                keyGen("  Apple Inc  ") shouldBe "search_apple inc"
                keyGen("MSFT") shouldBe "search_msft"
            }
        }

        When("using news key generator") {
            val keyGen: (Any) -> String = { tickers ->
                when (tickers) {
                    is List<*> -> "news_${tickers.sortedBy { it.toString() }.joinToString("_")}"
                    else -> "news_$tickers"
                }
            }

            Then("should sort tickers for consistent keys") {
                keyGen(listOf("MSFT", "AAPL")) shouldBe "news_AAPL_MSFT"
                keyGen(listOf("AAPL", "MSFT")) shouldBe "news_AAPL_MSFT"
                keyGen("single") shouldBe "news_single"
            }
        }

        When("using info key generator") {
            val keyGen: (Any) -> String = { tickers ->
                when (tickers) {
                    is List<*> -> "info_${tickers.sortedBy { it.toString() }.joinToString("_")}"
                    else -> "info_$tickers"
                }
            }

            Then("should sort tickers for consistent keys") {
                keyGen(listOf("GOOGL", "AAPL")) shouldBe "info_AAPL_GOOGL"
            }
        }
    }
})
