package com.cashq.domain.calculation

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DenominationComprehensiveTest {

    @Test
    fun canonicalDenominations_containsAll11RequiredEntries() {
        assertEquals(11, Denomination.ALL_CANONICAL.size)

        // 6 Banknotes
        assertEquals(6, Denomination.ALL_NOTES.size)
        assertEquals(50000L, Denomination.NOTE_500.valueInPaise)
        assertEquals(20000L, Denomination.NOTE_200.valueInPaise)
        assertEquals(10000L, Denomination.NOTE_100.valueInPaise)
        assertEquals(5000L, Denomination.NOTE_50.valueInPaise)
        assertEquals(2000L, Denomination.NOTE_20.valueInPaise)
        assertEquals(1000L, Denomination.NOTE_10.valueInPaise)

        // 5 Coins
        assertEquals(5, Denomination.ALL_COINS.size)
        assertEquals(2000L, Denomination.COIN_20.valueInPaise)
        assertEquals(1000L, Denomination.COIN_10.valueInPaise)
        assertEquals(500L, Denomination.COIN_5.valueInPaise)
        assertEquals(200L, Denomination.COIN_2.valueInPaise)
        assertEquals(100L, Denomination.COIN_1.valueInPaise)
    }

    @Test
    fun everyDenomination_multipliedByQuantity_givesAccurateSubtotal() {
        // Test each denomination individually with quantity = 10
        val testCases = listOf(
            Denomination.NOTE_500 to 5000L,
            Denomination.NOTE_200 to 2000L,
            Denomination.NOTE_100 to 1000L,
            Denomination.NOTE_50 to 500L,
            Denomination.NOTE_20 to 200L,
            Denomination.NOTE_10 to 100L,
            Denomination.COIN_20 to 200L,
            Denomination.COIN_10 to 100L,
            Denomination.COIN_5 to 50L,
            Denomination.COIN_2 to 20L,
            Denomination.COIN_1 to 10L
        )

        for ((denom, expectedRupees) in testCases) {
            val quantities = mapOf(denom.id to 10)
            val result = CashCalculationEngine.calculate(
                quantities = quantities,
                coinsVisible = true,
                coinMode = CoinCountingMode.SEPARATE
            )
            assertEquals("Failed for ${denom.id}", expectedRupees, result.activeTotalRupees)
            assertEquals("Units failed for ${denom.id}", 10L, result.activeTotalUnits)
        }
    }

    @Test
    fun negativeQuantities_treatedAsZeroOrCoerced() {
        val quantities = mapOf(Denomination.NOTE_500.id to -5)
        val result = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )
        assertEquals(0L, result.activeTotalRupees)
        assertEquals(0L, result.activeTotalUnits)
    }

    @Test
    fun largeValidQuantities_calculatesCorrectlyWithoutOverflow() {
        // 100,000 notes of ₹500 = ₹5,00,00,000 (5 Crore Rupees = 5,000,000,000 paise)
        val quantities = mapOf(Denomination.NOTE_500.id to 100000)
        val result = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )
        assertEquals(50000000L, result.activeTotalRupees)
        assertEquals(5000000000L, result.activeTotalPaise)
        assertEquals(100000L, result.activeTotalUnits)
        assertFalse(result.hasOverflow)
    }

    @Test
    fun combinedMode_sumsNoteAndCoinQuantitiesCorrectly() {
        // 10 ₹20 notes + 15 ₹20 coins = 25 units x ₹20 = ₹500
        // 5 ₹10 notes + 20 ₹10 coins = 25 units x ₹10 = ₹250
        val separate = mapOf(
            Denomination.NOTE_20.id to 10,
            Denomination.COIN_20.id to 15,
            Denomination.NOTE_10.id to 5,
            Denomination.COIN_10.id to 20
        )

        val combined = CashCalculationEngine.convertSeparateToCombined(separate)

        assertEquals(25, combined[Denomination.COMBINED_20.id])
        assertEquals(25, combined[Denomination.COMBINED_10.id])

        val result = CashCalculationEngine.calculate(
            quantities = combined,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        assertEquals(750L, result.activeTotalRupees)
        assertEquals(50L, result.activeTotalUnits)
    }
}
