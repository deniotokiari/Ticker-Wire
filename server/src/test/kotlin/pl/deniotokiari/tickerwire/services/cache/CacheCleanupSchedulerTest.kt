package pl.deniotokiari.tickerwire.services.cache

import com.google.api.core.ApiFuture
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.QuerySnapshot
import com.google.cloud.firestore.WriteBatch
import com.google.cloud.firestore.WriteResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Duration

class CacheCleanupSchedulerTest : BehaviorSpec({

    @Serializable
    data class TestData(val id: String)

    fun createMockFirestore(): Firestore {
        val mockFirestore = mockk<Firestore>()
        val mockCollection = mockk<CollectionReference>()
        val mockQuery = mockk<Query>()
        val mockQuerySnapshot = mockk<QuerySnapshot>()
        val mockBatch = mockk<WriteBatch>()
        val mockBatchFuture = mockk<ApiFuture<List<WriteResult>>>()

        every { mockFirestore.collection(any()) } returns mockCollection
        every { mockCollection.whereLessThan(any<String>(), any()) } returns mockQuery
        
        val mockQueryFuture = mockk<ApiFuture<QuerySnapshot>>()
        every { mockQuery.get() } returns mockQueryFuture
        every { mockQueryFuture.get() } returns mockQuerySnapshot
        every { mockQuerySnapshot.documents } returns emptyList()
        
        every { mockFirestore.batch() } returns mockBatch
        every { mockBatch.commit() } returns mockBatchFuture
        every { mockBatchFuture.get() } returns emptyList()

        return mockFirestore
    }

    Given("CacheCleanupScheduler") {

        When("registering a cache") {
            val scheduler = CacheCleanupScheduler()
            val mockFirestore = createMockFirestore()
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofMinutes(30),
                firestore = mockFirestore,
            )

            scheduler.register("test", cache)

            Then("cache count should increase") {
                scheduler.registeredCacheCount() shouldBe 1
            }

            And("when unregistering") {
                scheduler.unregister("test")

                Then("cache count should decrease") {
                    scheduler.registeredCacheCount() shouldBe 0
                }
            }
        }

        When("running startup cleanup") {
            val scheduler = CacheCleanupScheduler()
            val mockFirestore = createMockFirestore()
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofMinutes(30),
                firestore = mockFirestore,
            )

            scheduler.register("test", cache)

            Then("cleanup should complete without errors") {
                scheduler.runStartupCleanup()
                // Give async cleanup time to run
                delay(100)
                // Verify Firestore was queried for expired documents
                verify { mockFirestore.collection("test_cache") }
            }
        }

        When("running startup cleanup with no caches registered") {
            val scheduler = CacheCleanupScheduler()

            Then("should complete without errors") {
                scheduler.runStartupCleanup()
                scheduler.registeredCacheCount() shouldBe 0
            }
        }

        When("shutting down") {
            val scheduler = CacheCleanupScheduler()
            val mockFirestore = createMockFirestore()
            val cache = FirestoreCacheService(
                name = "test_cache",
                serializer = TestData.serializer(),
                ttl = Duration.ofMinutes(30),
                firestore = mockFirestore,
            )

            scheduler.register("test", cache)
            scheduler.shutdown()

            Then("caches should be cleared") {
                scheduler.registeredCacheCount() shouldBe 0
            }
        }

        When("registering multiple caches") {
            val scheduler = CacheCleanupScheduler()
            val mockFirestore = createMockFirestore()

            val cache1 = FirestoreCacheService(
                name = "cache_1",
                serializer = TestData.serializer(),
                ttl = Duration.ofMinutes(30),
                firestore = mockFirestore,
            )
            val cache2 = FirestoreCacheService(
                name = "cache_2",
                serializer = TestData.serializer(),
                ttl = Duration.ofHours(1),
                firestore = mockFirestore,
            )

            scheduler.register("cache1", cache1)
            scheduler.register("cache2", cache2)

            Then("all caches should be registered") {
                scheduler.registeredCacheCount() shouldBe 2
            }

            And("startup cleanup should run for all caches") {
                scheduler.runStartupCleanup()
                // Give async cleanup time to run
                delay(100)
                verify { mockFirestore.collection("cache_1") }
                verify { mockFirestore.collection("cache_2") }
            }
        }
    }
})
