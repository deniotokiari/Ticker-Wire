package pl.deniotokiari.tickerwire.services.analytics

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import pl.deniotokiari.tickerwire.models.Provider

class ProviderAnalyticsServiceTest : BehaviorSpec({

    Given("ProviderAnalyticsService") {

        When("recording provider selections") {
            val analytics = ProviderAnalyticsService()

            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.NEWS)
            analytics.recordProviderSelection(Provider.MASSIVE, OperationType.INFO)

            val snapshot = analytics.getSnapshot()

            Then("should track selection counts correctly") {
                snapshot.providerStats["FINNHUB"]!!.searchSelections shouldBe 2
                snapshot.providerStats["FINNHUB"]!!.newsSelections shouldBe 1
                snapshot.providerStats["FINNHUB"]!!.totalSelections shouldBe 3

                snapshot.providerStats["MASSIVE"]!!.infoSelections shouldBe 1
                snapshot.providerStats["MASSIVE"]!!.totalSelections shouldBe 1
            }

            Then("should track operation counts") {
                snapshot.operationStats.totalSearchRequests shouldBe 2
                snapshot.operationStats.totalNewsRequests shouldBe 1
                snapshot.operationStats.totalInfoRequests shouldBe 1
                snapshot.operationStats.totalRequests shouldBe 4
            }

            Then("should track last selected timestamp") {
                snapshot.providerStats["FINNHUB"]!!.lastSelectedAt shouldNotBe null
            }
        }

        When("recording provider failures") {
            val analytics = ProviderAnalyticsService()

            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordProviderFailure(Provider.FINNHUB, OperationType.SEARCH)

            val snapshot = analytics.getSnapshot()

            Then("should track failure counts") {
                snapshot.providerStats["FINNHUB"]!!.searchFailures shouldBe 1
                snapshot.providerStats["FINNHUB"]!!.totalFailures shouldBe 1
            }

            Then("should calculate success rate correctly") {
                // 2 selections, 1 failure = 50% success
                snapshot.providerStats["FINNHUB"]!!.successRate shouldBe 50.0
            }

            Then("should track last failed timestamp") {
                snapshot.providerStats["FINNHUB"]!!.lastFailedAt shouldNotBe null
            }
        }

        When("recording cache hits and misses") {
            val analytics = ProviderAnalyticsService()

            // Search: 3 hits, 1 miss = 75% hit rate
            analytics.recordCacheHit(OperationType.SEARCH)
            analytics.recordCacheHit(OperationType.SEARCH)
            analytics.recordCacheHit(OperationType.SEARCH)
            analytics.recordCacheMiss(OperationType.SEARCH)

            // News: 1 hit, 1 miss = 50% hit rate
            analytics.recordCacheHit(OperationType.NEWS)
            analytics.recordCacheMiss(OperationType.NEWS)

            // Info: 0 hits, 2 misses = 0% hit rate
            analytics.recordCacheMiss(OperationType.INFO)
            analytics.recordCacheMiss(OperationType.INFO)

            val snapshot = analytics.getSnapshot()

            Then("should track search cache stats") {
                snapshot.cacheStats.searchCacheHits shouldBe 3
                snapshot.cacheStats.searchCacheMisses shouldBe 1
                snapshot.cacheStats.searchCacheHitRate shouldBe 75.0
            }

            Then("should track news cache stats") {
                snapshot.cacheStats.newsCacheHits shouldBe 1
                snapshot.cacheStats.newsCacheMisses shouldBe 1
                snapshot.cacheStats.newsCacheHitRate shouldBe 50.0
            }

            Then("should track info cache stats") {
                snapshot.cacheStats.infoCacheHits shouldBe 0
                snapshot.cacheStats.infoCacheMisses shouldBe 2
                snapshot.cacheStats.infoCacheHitRate shouldBe 0.0
            }

            Then("should calculate overall cache hit rate") {
                // Total: 4 hits, 4 misses = 50%
                snapshot.cacheStats.totalCacheHits shouldBe 4
                snapshot.cacheStats.totalCacheMisses shouldBe 4
                snapshot.cacheStats.overallCacheHitRate shouldBe 50.0
            }
        }

        When("recording latencies") {
            val analytics = ProviderAnalyticsService()

            analytics.recordLatency(OperationType.SEARCH, 100)
            analytics.recordLatency(OperationType.SEARCH, 200)
            analytics.recordLatency(OperationType.NEWS, 150)
            analytics.recordLatency(OperationType.INFO, 50)

            val snapshot = analytics.getSnapshot()

            Then("should calculate average latencies") {
                snapshot.operationStats.averageSearchLatencyMs shouldBe 150.0
                snapshot.operationStats.averageNewsLatencyMs shouldBe 150.0
                snapshot.operationStats.averageInfoLatencyMs shouldBe 50.0
            }
        }

        When("resetting analytics") {
            val analytics = ProviderAnalyticsService()

            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordCacheHit(OperationType.SEARCH)
            analytics.recordLatency(OperationType.SEARCH, 100)

            analytics.reset()
            val snapshot = analytics.getSnapshot()

            Then("should clear all counters") {
                snapshot.providerStats.isEmpty() shouldBe true
                snapshot.operationStats.totalRequests shouldBe 0
                snapshot.cacheStats.totalCacheHits shouldBe 0
            }
        }

        When("getting snapshot with no data") {
            val analytics = ProviderAnalyticsService()
            val snapshot = analytics.getSnapshot()

            Then("should return empty stats") {
                snapshot.providerStats.isEmpty() shouldBe true
                snapshot.operationStats.totalRequests shouldBe 0
                snapshot.cacheStats.overallCacheHitRate shouldBe 0.0
            }

            Then("should have valid timestamp") {
                snapshot.timestamp shouldBeGreaterThan 0
            }

            Then("should have non-negative uptime") {
                snapshot.uptime shouldBeGreaterThan -1L
            }
        }

        When("tracking multiple providers") {
            val analytics = ProviderAnalyticsService()

            Provider.entries.forEach { provider ->
                analytics.recordProviderSelection(provider, OperationType.SEARCH)
            }

            val snapshot = analytics.getSnapshot()

            Then("should track all providers") {
                snapshot.providerStats.size shouldBe Provider.entries.size
            }
        }

        When("calculating success rate with 100% success") {
            val analytics = ProviderAnalyticsService()

            repeat(10) {
                analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            }

            val snapshot = analytics.getSnapshot()

            Then("success rate should be 100%") {
                snapshot.providerStats["FINNHUB"]!!.successRate shouldBe 100.0
            }
        }

        When("calculating success rate with all failures") {
            val analytics = ProviderAnalyticsService()

            analytics.recordProviderSelection(Provider.FINNHUB, OperationType.SEARCH)
            analytics.recordProviderFailure(Provider.FINNHUB, OperationType.SEARCH)

            val snapshot = analytics.getSnapshot()

            Then("success rate should be 0%") {
                snapshot.providerStats["FINNHUB"]!!.successRate shouldBe 0.0
            }
        }
    }
})

