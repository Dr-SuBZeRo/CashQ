package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSummaryGeneratorTest {

    @Test
    fun generateSummary_separateMode_containsBothNotesAndCoins() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 74,
            Denomination.NOTE_200.id to 14,
            Denomination.COIN_20.id to 5,
            Denomination.COIN_5.id to 20
        )

        val summary = ShareSummaryGenerator.generateSummary(
            title = "Clinic Closing Cash",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertTrue(summary.contains("CASHQ — CASH COUNT SUMMARY"))
        assertTrue(summary.contains("Session: Clinic Closing Cash"))
        assertTrue(summary.contains("BANKNOTES"))
        assertTrue(summary.contains("COINS"))
        assertTrue(summary.contains("₹500 × 74"))
        assertTrue(summary.contains("₹200 × 14"))
        assertTrue(summary.contains("₹20 × 5"))
        assertTrue(summary.contains("₹5 × 20"))
        assertTrue(summary.contains("TOTAL CASH:"))
        assertTrue(summary.contains("Created with CashQ by SuBZeRo"))
    }

    @Test
    fun generateSummary_coinsDisabled_marksNotesOnly() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 10,
            Denomination.COIN_5.id to 100
        )

        val summary = ShareSummaryGenerator.generateSummary(
            title = "Morning Register",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = false,
            coinMode = CoinCountingMode.SEPARATE
        )

        assertTrue(summary.contains("Mode: Banknotes Only (Coins Hidden)"))
        assertTrue(summary.contains("TOTAL CASH (Notes Only):"))
        // Excluded coins should not have a section
        assertTrue(!summary.contains("\nCOINS\n"))
    }
}
