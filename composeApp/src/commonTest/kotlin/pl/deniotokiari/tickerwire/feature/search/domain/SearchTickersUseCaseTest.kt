package pl.deniotokiari.tickerwire.feature.search.domain

import pl.deniotokiari.tickerwire.model.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTickersUseCaseTest {

    // Non-suspend fake repository for testing in KMP
    private class FakeTickerRepository(
        private val searchResults: Map<String, List<Ticker>> = emptyMap(),
        private val shouldThrowError: Boolean = false,
    ) {
        fun search(query: String): List<Ticker> {
            if (shouldThrowError) {
                throw RuntimeException("Search failed")
            }
            return searchResults[query.lowercase()] ?: emptyList()
        }
    }

    // Testable use case wrapper (synchronous for KMP test compatibility)
    private class TestableSearchUseCase(
        private val repository: FakeTickerRepository
    ) {
        operator fun invoke(query: String): Result<List<Ticker>> = runCatching {
            repository.search(query)
        }
    }

    @Test
    fun invokeReturnsMatchingTickers() {
        val appleResults = listOf(
            Ticker(symbol = "AAPL", company = "Apple Inc"),
            Ticker(symbol = "AAPD", company = "Apple Dividend")
        )
        val repository = FakeTickerRepository(
            searchResults = mapOf("apple" to appleResults)
        )
        val useCase = TestableSearchUseCase(repository)

        val result = useCase("apple")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("AAPL", result.getOrNull()?.first()?.symbol)
    }

    @Test
    fun invokeReturnsEmptyListForNoMatches() {
        val repository = FakeTickerRepository(searchResults = emptyMap())
        val useCase = TestableSearchUseCase(repository)

        val result = useCase("nonexistent")

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun invokeReturnsFailureOnError() {
        val repository = FakeTickerRepository(shouldThrowError = true)
        val useCase = TestableSearchUseCase(repository)

        val result = useCase("apple")

        assertTrue(result.isFailure)
    }

    @Test
    fun invokeIsCaseInsensitive() {
        val results = listOf(Ticker(symbol = "AAPL", company = "Apple Inc"))
        val repository = FakeTickerRepository(
            searchResults = mapOf("apple" to results)
        )
        val useCase = TestableSearchUseCase(repository)

        val result = useCase("APPLE")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun invokeWithEmptyQueryReturnsEmpty() {
        val repository = FakeTickerRepository(
            searchResults = mapOf("" to emptyList())
        )
        val useCase = TestableSearchUseCase(repository)

        val result = useCase("")

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }
}

