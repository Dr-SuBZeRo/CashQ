package com.cashq.domain.calculation

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashCalculationEngineTest {

    @Test
    fun allDenominations_emptyOrZero_returnsZeroTotal() {
        val result = CashCalculationEngine.calculate(
            quantities = emptyMap(),
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertEquals(0L, result.activeTotalPaise)
        assertEquals(0L, result.activeTotalRupees)
        assertEquals(0L, result.activeTotalUnits)
        assertFalse(result.isNotesOnly)
        assertFalse(result.hasOverflow)
    }

    @Test
    fun singleDenomination_calculatesSubtotalCorrectly() {
        // 74 x ₹500 = ₹37,000 = 3,700,000 paise
        val quantities = mapOf(Denomination.NOTE_500.id to 74)
        val result = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertEquals(3700000L, result.denominationSubtotals[Denomination.NOTE_500.id])
        assertEquals(37000L, result.activeTotalRupees)
        assertEquals(74L, result.activeTotalUnits)
        assertEquals(37000L, result.noteTotalPaise / 100L)
        assertEquals(0L, result.coinTotalPaise)
    }

    @Test
    fun mixedNotesAndCoins_calculatesExactTotal() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 74,  // 37,000
            Denomination.NOTE_200.id to 14,  // 2,800
            Denomination.NOTE_100.id to 1,   // 100
            Denomination.NOTE_50.id to 204,  // 10,200
            Denomination.NOTE_20.id to 262,  // 5,240
            Denomination.NOTE_10.id to 0,    // 0
            Denomination.COIN_20.id to 5,    // 100
            Denomination.COIN_10.id to 10,   // 100
            Denomination.COIN_5.id to 20,    // 100
            Denomination.COIN_2.id to 25,    // 50
            Denomination.COIN_1.id to 50     // 50
        )

        val result = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        val expectedNoteRupees = 37000L + 2800L + 100L + 10200L + 5240L + 0L // 55,340
        val expectedCoinRupees = 100L + 100L + 100L + 50L + 50L             // 400
        val expectedTotalRupees = expectedNoteRupees + expectedCoinRupees     // 55,740

        assertEquals(expectedTotalRupees, result.activeTotalRupees)
        assertEquals(expectedNoteRupees, result.noteTotalPaise / 100L)
        assertEquals(expectedCoinRupees, result.coinTotalPaise / 100L)
        assertEquals(74 + 14 + 1 + 204 + 262 + 0 + 5 + 10 + 20 + 25 + 50L, result.activeTotalUnits)
        assertFalse(result.isNotesOnly)
    }

    @Test
    fun coinsDisabled_excludesCoinsFromActiveTotal_preservesHiddenQuantities() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 10, // ₹5,000
            Denomination.COIN_5.id to 100   // ₹500
        )

        val result = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = false,
            coinMode = CoinCountingMode.SEPARATE
        )

        // Only notes contribute to active totals
        assertEquals(5000L, result.activeTotalRupees)
        assertEquals(10L, result.activeTotalUnits)
        assertEquals(5000L, result.noteTotalPaise / 100L)
        assertEquals(0L, result.coinTotalPaise)
        assertTrue(result.isNotesOnly)

        // The hidden coin quantity is still intact in the source map
        assertEquals(100, quantities[Denomination.COIN_5.id])

        // When coins are re-enabled, total restores seamlessly
        val reEnabled = CashCalculationEngine.calculate(
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )
        assertEquals(5500L, reEnabled.activeTotalRupees)
        assertEquals(110L, reEnabled.activeTotalUnits)
        assertFalse(reEnabled.isNotesOnly)
    }

    @Test
    fun separateToCombined_mergesNotesAndCoinsAtomically() {
        // 10 x ₹20 note + 15 x ₹20 coin = 25 units x ₹20 = ₹500
        // 5 x ₹10 note + 20 x ₹10 coin = 25 units x ₹10 = ₹250
        val separateMap = mapOf(
            Denomination.NOTE_500.id to 2,
            Denomination.NOTE_20.id to 10,
            Denomination.COIN_20.id to 15,
            Denomination.NOTE_10.id to 5,
            Denomination.COIN_10.id to 20,
            Denomination.COIN_5.id to 8
        )

        val beforeResult = CashCalculationEngine.calculate(
            quantities = separateMap,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        val combinedMap = CashCalculationEngine.convertSeparateToCombined(separateMap)

        assertEquals(25, combinedMap[Denomination.COMBINED_20.id])
        assertEquals(25, combinedMap[Denomination.COMBINED_10.id])
        assertFalse(combinedMap.containsKey(Denomination.NOTE_20.id))
        assertFalse(combinedMap.containsKey(Denomination.COIN_20.id))

        val afterResult = CashCalculationEngine.calculate(
            quantities = combinedMap,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        // Total monetary value is strictly identical
        assertEquals(beforeResult.activeTotalPaise, afterResult.activeTotalPaise)
        assertEquals(beforeResult.activeTotalRupees, afterResult.activeTotalRupees)
        assertEquals(beforeResult.activeTotalUnits, afterResult.activeTotalUnits)
    }

    @Test
    fun combinedToSeparate_assignsToNotes_setsCoinsToZero_preservesValue() {
        val combinedMap = mapOf(
            Denomination.NOTE_500.id to 2,
            Denomination.COMBINED_20.id to 25, // ₹500
            Denomination.COMBINED_10.id to 25  // ₹250
        )

        val beforeResult = CashCalculationEngine.calculate(
            quantities = combinedMap,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        val separateMap = CashCalculationEngine.convertCombinedToSeparate(combinedMap)

        assertEquals(25, separateMap[Denomination.NOTE_20.id])
        assertEquals(0, separateMap[Denomination.COIN_20.id])
        assertEquals(25, separateMap[Denomination.NOTE_10.id])
        assertEquals(0, separateMap[Denomination.COIN_10.id])

        val afterResult = CashCalculationEngine.calculate(
            quantities = separateMap,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        // Total monetary value must remain identical
        assertEquals(beforeResult.activeTotalPaise, afterResult.activeTotalPaise)
        assertEquals(beforeResult.activeTotalRupees, afterResult.activeTotalRupees)
        assertEquals(beforeResult.activeTotalUnits, afterResult.activeTotalUnits)
    }

    @Test
    fun overflowHandling_detectsOverflowSafely() {
        val extremeQuantities = mapOf(
            Denomination.NOTE_500.id to Int.MAX_VALUE
        )
        // Int.MAX_VALUE * 50,000 paise = 2,147,483,647 * 50,000 = ~107 trillion paise,
        // which fits in Long (up to 9.22 quintillion).
        val result = CashCalculationEngine.calculate(
            quantities = extremeQuantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertFalse(result.hasOverflow)
        assertEquals(Int.MAX_VALUE.toLong() * 500L, result.activeTotalRupees)
    }
}
