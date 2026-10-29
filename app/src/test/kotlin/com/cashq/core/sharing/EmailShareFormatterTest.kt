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
        assertTrue(html.contains("💰 CashQ"))
        assertTrue(html.contains("Indian Rupee Cash Counter"))

        // Escaped title
        assertTrue(html.contains("Store &amp; Clinic &lt;Evening&gt;"))
        assertFalse(html.contains("<Evening>"))

        // Grand total
        assertTrue(html.contains("₹4,000"))
        assertTrue(html.contains("257 units counted"))

        // Subtotals
        assertTrue(html.contains("Notes Subtotal:</strong> ₹2,000 (7 units)"))
        assertTrue(html.contains("Coins Subtotal:</strong> ₹2,000 (250 units)"))

        // Denominations table
        assertTrue(html.contains("₹500 Note"))
        assertTrue(html.contains("× 2"))
        assertTrue(html.contains("₹1,000"))

        // Footer attribution
        assertTrue(html.contains("SuBZeRo"))
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
        assertFalse(html.contains("Coins Subtotal:"))
    }
}
