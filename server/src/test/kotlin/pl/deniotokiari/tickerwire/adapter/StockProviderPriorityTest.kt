package pl.deniotokiari.tickerwire.adapter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import pl.deniotokiari.tickerwire.models.Provider

/**
 * Tests for StockProvider priority constants
 * Verifies that priority rankings are correctly defined
 */
class StockProviderPriorityTest : BehaviorSpec({

    Given("SEARCH_PRIORITY ranking") {

        Then("FINNHUB should have highest priority (1)") {
            StockProvider.SEARCH_PRIORITY[Provider.FINNHUB] shouldBe 1
        }

        Then("MASSIVE should have second priority (2)") {
            StockProvider.SEARCH_PRIORITY[Provider.MASSIVE] shouldBe 2
        }

        Then("FINANCIALMODELINGPREP should have priority 3") {
            StockProvider.SEARCH_PRIORITY[Provider.FINANCIALMODELINGPREP] shouldBe 3
        }

        Then("STOCKDATA should have priority 4") {
            StockProvider.SEARCH_PRIORITY[Provider.STOCKDATA] shouldBe 4
        }

        Then("MARKETAUX should have priority 5") {
            StockProvider.SEARCH_PRIORITY[Provider.MARKETAUX] shouldBe 5
        }

        Then("ALPHAVANTAGE should have priority 6") {
            StockProvider.SEARCH_PRIORITY[Provider.ALPHAVANTAGE] shouldBe 6
        }

        Then("MARKETSTACK should have lowest priority (7)") {
            StockProvider.SEARCH_PRIORITY[Provider.MARKETSTACK] shouldBe 7
        }

        Then("should cover all 7 providers") {
            StockProvider.SEARCH_PRIORITY.size shouldBe 7
        }
    }

    Given("NEWS_PRIORITY ranking") {

        Then("FINNHUB should have highest priority (1)") {
            StockProvider.NEWS_PRIORITY[Provider.FINNHUB] shouldBe 1
        }

        Then("MASSIVE should have second priority (2)") {
            StockProvider.NEWS_PRIORITY[Provider.MASSIVE] shouldBe 2
        }

        Then("STOCKDATA should have priority 3") {
            StockProvider.NEWS_PRIORITY[Provider.STOCKDATA] shouldBe 3
        }

        Then("MARKETAUX should have priority 4") {
            StockProvider.NEWS_PRIORITY[Provider.MARKETAUX] shouldBe 4
        }

        Then("ALPHAVANTAGE should have lowest priority (5)") {
            StockProvider.NEWS_PRIORITY[Provider.ALPHAVANTAGE] shouldBe 5
        }

        Then("should cover 5 providers (no FMP, MarketStack)") {
            StockProvider.NEWS_PRIORITY.size shouldBe 5
        }

        Then("FINANCIALMODELINGPREP should NOT be in NEWS_PRIORITY") {
            StockProvider.NEWS_PRIORITY.containsKey(Provider.FINANCIALMODELINGPREP) shouldBe false
        }

        Then("MARKETSTACK should NOT be in NEWS_PRIORITY") {
            StockProvider.NEWS_PRIORITY.containsKey(Provider.MARKETSTACK) shouldBe false
        }
    }

    Given("INFO_PRIORITY ranking") {

        Then("FINNHUB should have highest priority (1)") {
            StockProvider.INFO_PRIORITY[Provider.FINNHUB] shouldBe 1
        }

        Then("MASSIVE should have second priority (2)") {
            StockProvider.INFO_PRIORITY[Provider.MASSIVE] shouldBe 2
        }

        Then("FINANCIALMODELINGPREP should have priority 3") {
            StockProvider.INFO_PRIORITY[Provider.FINANCIALMODELINGPREP] shouldBe 3
        }

        Then("STOCKDATA should have priority 4") {
            StockProvider.INFO_PRIORITY[Provider.STOCKDATA] shouldBe 4
        }

        Then("MARKETSTACK should have priority 5") {
            StockProvider.INFO_PRIORITY[Provider.MARKETSTACK] shouldBe 5
        }

        Then("ALPHAVANTAGE should have lowest priority (6)") {
            StockProvider.INFO_PRIORITY[Provider.ALPHAVANTAGE] shouldBe 6
        }

        Then("should cover 6 providers (no MarketAux)") {
            StockProvider.INFO_PRIORITY.size shouldBe 6
        }

        Then("MARKETAUX should NOT be in INFO_PRIORITY") {
            StockProvider.INFO_PRIORITY.containsKey(Provider.MARKETAUX) shouldBe false
        }
    }

    Given("Priority consistency across functions") {

        Then("Tier 1 providers (FINNHUB, MASSIVE) should always be top 2 in all rankings") {
            // FINNHUB
            StockProvider.SEARCH_PRIORITY[Provider.FINNHUB]!! shouldBe 1
            StockProvider.NEWS_PRIORITY[Provider.FINNHUB]!! shouldBe 1
            StockProvider.INFO_PRIORITY[Provider.FINNHUB]!! shouldBe 1

            // MASSIVE
            StockProvider.SEARCH_PRIORITY[Provider.MASSIVE]!! shouldBe 2
            StockProvider.NEWS_PRIORITY[Provider.MASSIVE]!! shouldBe 2
            StockProvider.INFO_PRIORITY[Provider.MASSIVE]!! shouldBe 2
        }

        Then("ALPHAVANTAGE should always be lowest or near-lowest priority") {
            val searchPriority = StockProvider.SEARCH_PRIORITY[Provider.ALPHAVANTAGE]!!
            val newsPriority = StockProvider.NEWS_PRIORITY[Provider.ALPHAVANTAGE]!!
            val infoPriority = StockProvider.INFO_PRIORITY[Provider.ALPHAVANTAGE]!!

            // Should be in bottom tier (last or second-to-last)
            (searchPriority >= 6) shouldBe true
            (newsPriority >= 5) shouldBe true
            (infoPriority >= 6) shouldBe true
        }
    }
})

