package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode

/**
 * Formats [CashCountShareData] into a structured, Unicode and emoji-formatted plain text message
 * suitable for sharing via WhatsApp, Telegram, SMS, and other messaging apps.
 */
object RichTextShareFormatter {

    private const val DIVIDER = "━━━━━━━━━━━━━━━━━━"

    fun format(data: CashCountShareData): String {
        val sb = StringBuilder()
        sb.append("💰 CASHQ — CASH COUNT SUMMARY\n")
        sb.append("Session: ${data.title}\n")
        sb.append("📅 ${data.formattedDateTime}\n")

        if (data.isNotesOnly || !data.coinsVisible) {
            sb.append("Mode: Banknotes Only (Coins Hidden)\n")
        } else if (data.coinMode == CoinCountingMode.COMBINED) {
            sb.append("Mode: Combined ₹20 & ₹10 Denominations\n")
        }

        sb.append(DIVIDER).append("\n")

        // Banknotes section
        sb.append("💵 BANKNOTES\n")
        for (item in data.noteItems) {
            sb.append("${item.shortLabel} × ${item.quantity} = ${item.formattedSubtotal}\n")
        }
        sb.append("Notes subtotal: ${data.formattedNoteTotalRupees} · ${data.formattedNoteUnits} units\n")

        // Coins section (only if coins are active and visible)
        if (data.coinsVisible && !data.isNotesOnly && data.coinItems.isNotEmpty()) {
            sb.append("\n🪙 COINS\n")
            for (item in data.coinItems) {
                sb.append("${item.shortLabel} × ${item.quantity} = ${item.formattedSubtotal}\n")
            }
            sb.append("Coins subtotal: ${data.formattedCoinTotalRupees} · ${data.formattedCoinUnits} units\n")
        }

        sb.append(DIVIDER).append("\n")
        sb.append("💎 ${data.totalLabel}: ${data.formattedTotalRupees}\n")
        sb.append("📦 Total units: ${data.formattedTotalUnits}\n")
        sb.append(DIVIDER).append("\n")
        sb.append("Created with CashQ by SuBZeRo\n")

        return sb.toString()
    }
}
