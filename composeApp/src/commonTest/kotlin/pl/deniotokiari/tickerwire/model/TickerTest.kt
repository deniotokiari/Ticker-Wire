package pl.deniotokiari.tickerwire.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TickerTest {

    @Test
    fun tickerCreationWithValidData() {
        val ticker = Ticker(symbol = "AAPL", company = "Apple Inc")

        assertEquals("AAPL", ticker.symbol)
        assertEquals("Apple Inc", ticker.company)
    }

    @Test
    fun tickerEqualityWorksCorrectly() {
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker3 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")

        assertEquals(ticker1, ticker2)
        assertNotEquals(ticker1, ticker3)
    }

    @Test
    fun tickerHashCodeIsConsistent() {
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "AAPL", company = "Apple Inc")

        assertEquals(ticker1.hashCode(), ticker2.hashCode())
    }

    @Test
    fun tickerWorksInCollections() {
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "AAPL", company = "Apple Inc")
        
        val set = setOf(ticker1, ticker2)
        
        assertEquals(1, set.size) // Should be deduplicated
    }

    @Test
    fun tickerRemovalFromList() {
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        val ticker3 = Ticker(symbol = "MSFT", company = "Microsoft")
        
        val list = listOf(ticker1, ticker2, ticker3)
        val tickerToRemove = Ticker(symbol = "GOOGL", company = "Alphabet Inc")
        
        val result = list - tickerToRemove
        
        assertEquals(2, result.size)
        assertTrue(result.contains(ticker1))
        assertTrue(result.contains(ticker3))
    }

    @Test
    fun tickerCopyWorks() {
        val original = Ticker(symbol = "AAPL", company = "Apple Inc")
        val copied = original.copy(symbol = "AAPL.MX")

        assertEquals("AAPL.MX", copied.symbol)
        assertEquals("Apple Inc", copied.company)
        assertNotEquals(original, copied)
    }

    @Test
    fun tickerWithDifferentCompanyIsNotEqual() {
        val ticker1 = Ticker(symbol = "AAPL", company = "Apple Inc")
        val ticker2 = Ticker(symbol = "AAPL", company = "Apple Inc.")

        assertNotEquals(ticker1, ticker2)
    }
}

