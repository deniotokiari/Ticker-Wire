package pl.deniotokiari.tickerwire.model

import pl.deniotokiari.tickerwire.model.dto.TickerInfoDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TickerInfoDtoTest {

    @Test
    fun tickerInfoDtoCreationWithValidData() {
        val info = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        assertEquals("150.00", info.marketValueFormatted)
        assertEquals("+2.00", info.deltaFormatted)
        assertEquals("+1.35%", info.percentFormatted)
        assertEquals("USD", info.currency)
    }

    @Test
    fun tickerInfoDtoEquality() {
        val info1 = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val info2 = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val info3 = TickerInfoDto(
            marketValueFormatted = "200.00",
            deltaFormatted = "-5.00",
            percentFormatted = "-2.44%",
            currency = "USD"
        )

        assertEquals(info1, info2)
        assertNotEquals(info1, info3)
    }

    @Test
    fun tickerInfoDtoCopy() {
        val original = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val copy = original.copy(marketValueFormatted = "160.00")

        assertEquals("160.00", copy.marketValueFormatted)
        assertEquals(original.deltaFormatted, copy.deltaFormatted)
        assertEquals(original.percentFormatted, copy.percentFormatted)
        assertEquals(original.currency, copy.currency)
    }

    @Test
    fun tickerInfoDtoHashCodeConsistency() {
        val info1 = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val info2 = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun tickerInfoDtoWithDifferentCurrencies() {
        val usdInfo = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val eurInfo = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "EUR"
        )

        assertNotEquals(usdInfo, eurInfo)
    }

    @Test
    fun tickerInfoDtoWithNegativeValues() {
        val info = TickerInfoDto(
            marketValueFormatted = "95.50",
            deltaFormatted = "-4.50",
            percentFormatted = "-4.50%",
            currency = "USD"
        )

        assertTrue(info.deltaFormatted.startsWith("-"))
        assertTrue(info.percentFormatted.contains("-"))
    }

    @Test
    fun tickerInfoDtoWithLargeValues() {
        val info = TickerInfoDto(
            marketValueFormatted = "3,456.78",
            deltaFormatted = "+123.45",
            percentFormatted = "+3.70%",
            currency = "USD"
        )

        assertEquals("3,456.78", info.marketValueFormatted)
        assertEquals("+123.45", info.deltaFormatted)
    }

    @Test
    fun tickerInfoDtoInMap() {
        val appleInfo = TickerInfoDto(
            marketValueFormatted = "150.00",
            deltaFormatted = "+2.00",
            percentFormatted = "+1.35%",
            currency = "USD"
        )

        val googleInfo = TickerInfoDto(
            marketValueFormatted = "2800.00",
            deltaFormatted = "-15.00",
            percentFormatted = "-0.53%",
            currency = "USD"
        )

        val infoMap = mapOf(
            "AAPL" to appleInfo,
            "GOOGL" to googleInfo
        )

        assertEquals(appleInfo, infoMap["AAPL"])
        assertEquals(googleInfo, infoMap["GOOGL"])
        assertEquals(2, infoMap.size)
    }
}
