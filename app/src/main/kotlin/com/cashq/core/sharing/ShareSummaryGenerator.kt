package com.cashq.core.sharing

import com.cashq.core.formatting.CurrencyFormatter
import com.cashq.domain.calculation.CashCalculationEngine
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates plain-text summaries of counting sessions suitable for Android's native share sheet.
 */
object ShareSummaryGenerator {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.forLanguageTag("en-IN"))

    /**
     * Generates a plain-text summary from session parameters.
     *
     * @param title User-defined or auto-generated session title.
     * @param timestamp Epoch millis when the session was created/saved.
     * @param quantities Map of denomination ID to quantity.
     * @param coinsVisible Whether coins are included in the count.
     * @param coinMode SEPARATE or COMBINED counting mode.
     * @return Formatted plain-text summary.
     */
    fun generateSummary(
        title: String,
        timestamp: Long,
        quantities: Map<String, Int>,
        coinsVisible: Boolean,
        coinMode: CoinCountingMode
    ): String {
        val calc = CashCalculationEngine.calculate(quantities, coinsVisible, coinMode)
        val visibleDenoms = CashCalculationEngine.getVisibleDenominations(coinsVisible, coinMode)
        val dateStr = dateFormat.format(Date(timestamp))

        val notes = visibleDenoms.filter { it.type == CurrencyType.NOTE }
        val coins = visibleDenoms.filter { it.type == CurrencyType.COIN }

        val sb = StringBuilder()
        sb.append("CASHQ — CASH COUNT SUMMARY\n")
        sb.append("Session: $title\n")
        sb.append("Date: $dateStr\n")
        if (!coinsVisible) {
            sb.append("Mode: Banknotes Only (Coins Hidden)\n")
        } else if (coinMode == CoinCountingMode.COMBINED) {
            sb.append("Mode: Combined ₹20 & ₹10 Denominations\n")
        }
        sb.append("----------------------------------------\n")

        // Banknotes section
        sb.append("BANKNOTES\n")
        var noteUnitsSum = 0L
        for (denom in notes) {
            val qty = quantities[denom.id] ?: 0
            val subtotalRupees = denom.valueInRupees * qty
            noteUnitsSum += qty
            sb.append("${denom.shortLabel} × $qty = ${CurrencyFormatter.formatRupees(subtotalRupees)}\n")
        }
        sb.append("Subtotal Notes: ${CurrencyFormatter.formatRupees(calc.noteTotalPaise / 100L)} (${CurrencyFormatter.formatUnits(noteUnitsSum)} units)\n")

        // Coins section (if visible)
        if (coinsVisible && coins.isNotEmpty()) {
            sb.append("\nCOINS\n")
            var coinUnitsSum = 0L
            for (denom in coins) {
                val qty = quantities[denom.id] ?: 0
                val subtotalRupees = denom.valueInRupees * qty
                coinUnitsSum += qty
                sb.append("${denom.shortLabel} × $qty = ${CurrencyFormatter.formatRupees(subtotalRupees)}\n")
            }
            sb.append("Subtotal Coins: ${CurrencyFormatter.formatRupees(calc.coinTotalPaise / 100L)} (${CurrencyFormatter.formatUnits(coinUnitsSum)} units)\n")
        }

        sb.append("----------------------------------------\n")
        sb.append("Total Units Counted: ${CurrencyFormatter.formatUnits(calc.activeTotalUnits)}\n")
        if (!coinsVisible) {
            sb.append("TOTAL CASH (Notes Only): ${CurrencyFormatter.formatRupees(calc.activeTotalRupees)}\n")
        } else {
            sb.append("TOTAL CASH: ${CurrencyFormatter.formatRupees(calc.activeTotalRupees)}\n")
        }
        sb.append("----------------------------------------\n")
        sb.append("Created with CashQ by SuBZeRo\n")

        return sb.toString()
    }
}
