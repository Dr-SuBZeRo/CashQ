package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextShareFormatterTest {

    @Test
    fun format_separateMode_containsEmojisAndStructuredSections() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 2, // ₹1,000
            Denomination.NOTE_200.id to 5, // ₹1,000
            Denomination.COIN_20.id to 50, // ₹1,000
            Denomination.COIN_5.id to 200  // ₹1,000
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Clinic Closing",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        val text = RichTextShareFormatter.format(shareData)

        assertTrue(text.contains("💰 CASHQ — CASH COUNT SUMMARY"))
        assertTrue(text.contains("Session: Clinic Closing"))
        assertTrue(text.contains("📅"))
        assertTrue(text.contains("━━━━━━━━━━━━━━━━━━"))
        assertTrue(text.contains("💵 BANKNOTES"))
        assertTrue(text.contains("₹500 × 2 = ₹1,000"))
        assertTrue(text.contains("₹200 × 5 = ₹1,000"))
        assertTrue(text.contains("Notes subtotal: ₹2,000 · 7 units"))
        assertTrue(text.contains("🪙 COINS"))
        assertTrue(text.contains("₹20 × 50 = ₹1,000"))
        assertTrue(text.contains("₹5 × 200 = ₹1,000"))
        assertTrue(text.contains("Coins subtotal: ₹2,000 · 250 units"))
        assertTrue(text.contains("💎 TOTAL CASH: ₹4,000"))
        assertTrue(text.contains("📦 Total units: 257"))
        assertTrue(text.contains("Created with CashQ by SuBZeRo"))
    }

    @Test
    fun format_notesOnly_excludesCoinsAndMarksMode() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 10,
            Denomination.COIN_5.id to 100
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Morning Shift",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = false,
            coinMode = CoinCountingMode.SEPARATE
        )

        val text = RichTextShareFormatter.format(shareData)

        assertTrue(text.contains("Mode: Banknotes Only (Coins Hidden)"))
        assertTrue(text.contains("💵 BANKNOTES"))
        assertFalse(text.contains("🪙 COINS"))
        assertTrue(text.contains("💎 TOTAL CASH (Notes Only): ₹5,000"))
    }

    @Test
    fun format_combinedMode_indicatesCombinedMode() {
        val quantities = mapOf(
            Denomination.COMBINED_20.id to 25
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Combined Register",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        val text = RichTextShareFormatter.format(shareData)

        assertTrue(text.contains("Mode: Combined ₹20 & ₹10 Denominations"))
        assertTrue(text.contains("₹20 × 25 = ₹500"))
    }
}
