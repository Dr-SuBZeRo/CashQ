package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailShareFormatterTest {

    @Test
    fun formatSubject_formatsCleanSubjectLine() {
        val shareData = CashCountShareData.fromCounter(
            title = "Pharmacy Register",
            quantities = mapOf(Denomination.NOTE_500.id to 22),
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        val subject = EmailShareFormatter.formatSubject(shareData)
        assertEquals("Cash Count Summary: Pharmacy Register (₹11,000)", subject)
    }

    @Test
    fun escapeHtml_escapesSpecialCharactersCorrectly() {
        val dangerousInput = "<script>alert('test & \"attack\"');</script>"
        val escaped = EmailShareFormatter.escapeHtml(dangerousInput)

        assertEquals("&lt;script&gt;alert(&#39;test &amp; &quot;attack&quot;&#39;);&lt;/script&gt;", escaped)
        assertFalse(escaped.contains("<"))
        assertFalse(escaped.contains(">"))
    }

    @Test
    fun formatHtml_generatesValidResponsiveHtmlTable() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 2, // ₹1,000
            Denomination.NOTE_200.id to 5, // ₹1,000
            Denomination.COIN_20.id to 50, // ₹1,000
            Denomination.COIN_5.id to 200  // ₹1,000
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Store & Clinic <Evening>",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE
        )

        val html = EmailShareFormatter.formatHtml(shareData)

        // Basic HTML structure
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("</html>"))
        assertTrue(html.contains("CashQ"))
        assertTrue(html.contains("Indian Rupee Cash Counter"))
        assertTrue(html.contains("CASH COUNT REPORT"))

        // Escaped title
        assertTrue(html.contains("Store &amp; Clinic &lt;Evening&gt;"))
        assertFalse(html.contains("<Evening>"))

        // Grand total
        assertTrue(html.contains("₹4,000"))
        assertTrue(html.contains("257 units counted"))

        // Subtotals
        assertTrue(html.contains("Banknotes Subtotal:</strong>"))
        assertTrue(html.contains("₹2,000"))
        assertTrue(html.contains("Coins Subtotal:</strong>"))

        // 3-column table structure
        assertTrue(html.contains("<th style=\"padding:10px 14px;\">Denomination</th>"))
        assertTrue(html.contains("<th style=\"padding:10px 14px;text-align:right;\">Count</th>"))
        assertTrue(html.contains("<th style=\"padding:10px 14px;text-align:right;\">Subtotal</th>"))

        // Denominations table and section headings in separate mode
        assertTrue(html.contains("💵 Banknotes"))
        assertTrue(html.contains("🪙 Coins"))
        assertTrue(html.contains("₹500 Note"))
        assertTrue(html.contains("× 2"))
        assertTrue(html.contains("₹1,000"))

        // Final total closing summary & footer attribution
        assertTrue(html.contains("Final Total:"))
        assertTrue(html.contains("Created with <strong>CashQ</strong> by SuBZeRo"))
        assertTrue(html.contains("100% Offline &amp; Private"))
    }

    @Test
    fun formatHtml_notesOnly_containsNotesOnlyBanner() {
        val shareData = CashCountShareData.fromCounter(
            title = "Notes Only Count",
            timestamp = 1730180000000L,
            quantities = mapOf(Denomination.NOTE_500.id to 10),
            coinsVisible = false,
            coinMode = CoinCountingMode.SEPARATE
        )

        val html = EmailShareFormatter.formatHtml(shareData)

        assertTrue(html.contains("Mode: Banknotes Only (Coins Hidden)"))
        assertTrue(html.contains("TOTAL CASH (Notes Only)"))
        assertTrue(html.contains("NOTES ONLY · Coins excluded from count"))
        assertFalse(html.contains("🪙 Coins Subtotal:"))
        assertFalse(html.contains("🪙 Coins</th>"))
    }

    @Test
    fun formatHtml_combinedMode_displaysCombinedSectionHeadingAndMergedRows() {
        val quantities = mapOf(
            Denomination.NOTE_500.id to 2, // ₹1,000
            Denomination.COMBINED_20.id to 10, // ₹200
            Denomination.COMBINED_10.id to 5,  // ₹50
            Denomination.COIN_5.id to 20       // ₹100
        )

        val shareData = CashCountShareData.fromCounter(
            title = "Combined Register",
            timestamp = 1730180000000L,
            quantities = quantities,
            coinsVisible = true,
            coinMode = CoinCountingMode.COMBINED
        )

        val html = EmailShareFormatter.formatHtml(shareData)

        assertTrue(html.contains("Mode: Combined ₹20 &amp; ₹10 Denominations"))
        assertTrue(html.contains("💵 Banknotes &amp; Combined Denominations"))
        assertTrue(html.contains("₹20 (Note + Coin)"))
        assertTrue(html.contains("₹10 (Note + Coin)"))
        assertTrue(html.contains("🪙 Coins (₹5, ₹2, ₹1)"))
        assertTrue(html.contains("₹1,350"))
    }
}
