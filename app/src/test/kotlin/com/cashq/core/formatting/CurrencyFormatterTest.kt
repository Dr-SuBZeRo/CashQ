package com.cashq.core.formatting

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatRupees_zero_returnsRupeeZero() {
        assertEquals("₹0", CurrencyFormatter.formatRupees(0L))
    }

    @Test
    fun formatRupees_hundreds_formatsCorrectly() {
        assertEquals("₹500", CurrencyFormatter.formatRupees(500L))
    }

    @Test
    fun formatRupees_thousands_formatsWithComma() {
        assertEquals("₹37,000", CurrencyFormatter.formatRupees(37000L))
        assertEquals("₹2,800", CurrencyFormatter.formatRupees(2800L))
    }

    @Test
    fun formatRupees_lakhs_usesIndianGrouping() {
        // Indian grouping: 1,23,456 (not 123,456)
        assertEquals("₹1,23,456", CurrencyFormatter.formatRupees(123456L))
    }

    @Test
    fun formatRupees_crores_usesIndianGrouping() {
        assertEquals("₹1,23,45,678", CurrencyFormatter.formatRupees(12345678L))
    }

    @Test
    fun formatPaise_convertsAndFormatsCorrectly() {
        assertEquals("₹37,000", CurrencyFormatter.formatPaise(3700000L))
    }

    @Test
    fun formatUnits_formatsWithGrouping() {
        assertEquals("555", CurrencyFormatter.formatUnits(555L))
        assertEquals("1,234", CurrencyFormatter.formatUnits(1234L))
    }
}
