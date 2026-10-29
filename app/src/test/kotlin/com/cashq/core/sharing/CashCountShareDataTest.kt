package com.cashq.core.sharing

import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CashCountShareDataTest {

    @Test
    fun fromCounter_separateMode_calculatesCorrectTotalsAndItems() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 10, // ₹5,000
            Denomination.NOTE_200.id to 5,  // ₹1,000
            Denomination.COIN_20.id to 50,  // ₹1,000
            Denomination.COIN_5.id to 200   // ₹1,000
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Daily Register",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertEquals("Daily Register", shareData.title)
        assertEquals(8000L, shareData.totalRupees)
        assertEquals("₹8,000", shareData.formattedTotalRupees)
        assertEquals(265L, shareData.totalUnits)
        assertEquals(6000L, shareData.noteTotalRupees)
        assertEquals(15L, shareData.noteUnits)
        assertEquals(2000L, shareData.coinTotalRupees)
        assertEquals(250L, shareData.coinUnits)
        assertFalse(shareData.isNotesOnly)
        assertTrue(shareData.coinsVisible)
        assertEquals(CoinCountingMode.SEPARATE, shareData.coinMode)

        // Notes and coins lists
        assertEquals(6, shareData.noteItems.size) // 6 notes
        assertEquals(5, shareData.coinItems.size) // 5 coins
        assertEquals(11, shareData.visibleItems.size)
    }

    @Test
    fun fromCounter_notesOnly_excludesCoinsFromCalculationAndCoinList() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 4, // ₹2,000
            Denomination.COIN_20.id to 50  // ₹1,000 (hidden)
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Morning Shift",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = false,
            coinMode = CoinCountingMode.SEPARATE
        )

        // Only notes contribute to active totals
        assertEquals(2000L, shareData.totalRupees)
        assertEquals("₹2,000", shareData.formattedTotalRupees)
        assertEquals(4L, shareData.totalUnits)
        assertEquals(2000L, shareData.noteTotalRupees)
        assertEquals(4L, shareData.noteUnits)
        assertEquals(0L, shareData.coinTotalRupees)
        assertEquals(0L, shareData.coinUnits)
        assertTrue(shareData.isNotesOnly)
        assertFalse(shareData.coinsVisible)
        assertTrue(shareData.coinItems.isEmpty())
        assertEquals(6, shareData.visibleItems.size)
        assertEquals("TOTAL CASH (Notes Only)", shareData.totalLabel)
        assertEquals("Banknotes Only (Coins Hidden)", shareData.modeLabel)
    }

    @Test
    fun fromCounter_combinedMode_unifiesTwentyAndTen() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 2,       // ₹1,000
            Denomination.COMBINED_20.id to 50,  // ₹1,000
            Denomination.COMBINED_10.id to 100, // ₹1,000
            Denomination.COIN_5.id to 200       // ₹1,000
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Combined Shift",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        assertEquals(4000L, shareData.totalRupees)
        assertEquals("₹4,000", shareData.formattedTotalRupees)
        assertEquals(352L, shareData.totalUnits)
        assertEquals(CoinCountingMode.COMBINED, shareData.coinMode)

        // ₹20 and ₹10 combined rows appear in noteItems
        assertTrue(shareData.noteItems.any { it.id == Denomination.COMBINED_20.id })
        assertTrue(shareData.noteItems.any { it.id == Denomination.COMBINED_10.id })
        // Distinct coins for ₹20 and ₹10 must NOT appear in coinItems
        assertFalse(shareData.coinItems.any { it.id == Denomination.COIN_20.id })
        assertFalse(shareData.coinItems.any { it.id == Denomination.COIN_10.id })
        assertEquals(3, shareData.coinItems.size) // ₹5, ₹2, ₹1
    }

    @Test
    fun fromSavedSession_preservesSavedSnapshotIndependently() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 10,
            Denomination.COIN_5.id to 100
        )
        val entity = SavedSessionEntity(
            id = "test-session-1",
            title = "Saved Store Count",
            createdAt = 1730180000000L,
            updatedAt = 1730180000000L,
            totalPaise = 550000L, // ₹5,500
            totalUnits = 110L,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE.name,
            quantitiesJson = SavedSessionEntity.serializeQuantities(quantities),
            isNotesOnly = false
        )

        val shareData = CashCountShareData.fromSavedSession(entity)

        assertEquals("Saved Store Count", shareData.title)
        assertEquals(5500L, shareData.totalRupees)
        assertEquals("₹5,500", shareData.formattedTotalRupees)
        assertEquals(110L, shareData.totalUnits)
        assertEquals(5000L, shareData.noteTotalRupees)
        assertEquals(500L, shareData.coinTotalRupees)
    }

    @Test
    fun indianNumberGrouping_formatsCorrectly() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 226912 // ₹11,34,56,000 (11 crores 34 lakhs 56 thousand)
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Large Treasury",
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertEquals("₹11,34,56,000", shareData.formattedTotalRupees)
        assertEquals("2,26,912", shareData.formattedTotalUnits)
    }

    @Test
    fun zeroQuantities_handledGracefully() {
        val shareData = CashCountShareData.fromCounter(
            title = "",
            quantities = emptyMap(),
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertEquals("Cash Count Summary", shareData.title)
        assertEquals(0L, shareData.totalRupees)
        assertEquals("₹0", shareData.formattedTotalRupees)
        assertEquals(0L, shareData.totalUnits)
        assertEquals("0", shareData.formattedTotalUnits)
    }
}
