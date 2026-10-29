package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType

/**
 * Generates email-safe, responsive HTML tables and subjects for [CashCountShareData].
 */
object EmailShareFormatter {

    fun formatSubject(data: CashCountShareData): String {
        return "Cash Count Summary: ${data.title} (${data.formattedTotalRupees})"
    }

    /**
     * Escapes standard HTML special characters to prevent HTML/script injection.
     */
    fun escapeHtml(text: String): String {
        val out = StringBuilder(Math.max(16, text.length))
        for (c in text) {
            when (c) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&#39;")
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    /**
     * Renders a complete, standalone, responsive HTML email message with inline CSS.
     */
    fun formatHtml(data: CashCountShareData): String {
        val safeTitle = escapeHtml(data.title)
        val safeDate = escapeHtml(data.formattedDateTime)

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n")
        sb.append("<html lang=\"en\">\n")
        sb.append("<head>\n")
        sb.append("<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>Cash Count Summary</title>\n")
        sb.append("</head>\n")
        sb.append("<body style=\"margin:0;padding:20px;background-color:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;\">\n")

        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n")
        sb.append("<tr><td align=\"center\">\n")

        // Card container
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"max-width:580px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.05);border:1px solid #e2e8f0;\" cellpadding=\"0\" cellspacing=\"0\">\n")

        // Header block
        sb.append("<tr><td style=\"background:#0f172a;padding:24px 28px;text-align:left;\">\n")
        sb.append("<h1 style=\"margin:0 0 4px 0;font-size:22px;color:#ffffff;font-weight:700;letter-spacing:0.5px;\">💰 CashQ</h1>\n")
        sb.append("<p style=\"margin:0;font-size:13px;color:#94a3b8;\">Indian Rupee Cash Counter</p>\n")
        sb.append("</td></tr>\n")

        // Session Info block
        sb.append("<tr><td style=\"padding:24px 28px 16px 28px;\">\n")
        sb.append("<h2 style=\"margin:0 0 6px 0;font-size:18px;color:#0f172a;font-weight:600;\">$safeTitle</h2>\n")
        sb.append("<p style=\"margin:0 0 16px 0;font-size:13px;color:#64748b;\">📅 $safeDate</p>\n")

        if (data.isNotesOnly || !data.coinsVisible) {
            sb.append("<div style=\"display:inline-block;background:#fef3c7;color:#92400e;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;margin-bottom:16px;\">Mode: Banknotes Only (Coins Hidden)</div>\n")
        } else if (data.coinMode == CoinCountingMode.COMBINED) {
            sb.append("<div style=\"display:inline-block;background:#e0f2fe;color:#0369a1;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;margin-bottom:16px;\">Mode: Combined ₹20 &amp; ₹10 Denominations</div>\n")
        }

        // Grand Total Highlight Banner
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"background:#eff6ff;border-radius:8px;padding:16px;border:1px solid #bfdbfe;margin-bottom:20px;\" cellpadding=\"0\" cellspacing=\"0\">\n")
        sb.append("<tr><td align=\"center\" style=\"padding:8px;\">\n")
        sb.append("<div style=\"font-size:12px;text-transform:uppercase;color:#1e40af;font-weight:700;letter-spacing:1px;margin-bottom:4px;\">${escapeHtml(data.totalLabel)}</div>\n")
        sb.append("<div style=\"font-size:32px;font-weight:800;color:#1e3a8a;\">${escapeHtml(data.formattedTotalRupees)}</div>\n")
        sb.append("<div style=\"font-size:14px;color:#3b82f6;font-weight:600;margin-top:4px;\">📦 ${escapeHtml(data.formattedTotalUnits)} units counted</div>\n")
        sb.append("</td></tr>\n")
        sb.append("</table>\n")

        // Subtotals block
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"background:#f8fafc;border-radius:8px;padding:12px 16px;border:1px solid #e2e8f0;margin-bottom:20px;font-size:13px;\" cellpadding=\"0\" cellspacing=\"0\">\n")
        sb.append("<tr>\n")
        sb.append("<td style=\"padding:6px 0;\"><strong>💵 Notes Subtotal:</strong> ${escapeHtml(data.formattedNoteTotalRupees)} (${escapeHtml(data.formattedNoteUnits)} units)</td>\n")
        sb.append("</tr>\n")
        if (data.coinsVisible && !data.isNotesOnly && data.coinItems.isNotEmpty()) {
            sb.append("<tr>\n")
            sb.append("<td style=\"padding:6px 0;\"><strong>🪙 Coins Subtotal:</strong> ${escapeHtml(data.formattedCoinTotalRupees)} (${escapeHtml(data.formattedCoinUnits)} units)</td>\n")
            sb.append("</tr>\n")
        }
        sb.append("</table>\n")

        // Denomination Breakdown Table
        sb.append("<h3 style=\"margin:0 0 10px 0;font-size:14px;text-transform:uppercase;color:#475569;letter-spacing:0.5px;\">Denomination Breakdown</h3>\n")
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;font-size:13px;margin-bottom:24px;\">\n")
        sb.append("<tr style=\"background:#f1f5f9;color:#475569;text-align:left;font-weight:600;border-bottom:2px solid #cbd5e1;\">\n")
        sb.append("<th style=\"padding:8px 12px;\">Denomination</th>\n")
        sb.append("<th style=\"padding:8px 12px;text-align:center;\">Type</th>\n")
        sb.append("<th style=\"padding:8px 12px;text-align:right;\">Count</th>\n")
        sb.append("<th style=\"padding:8px 12px;text-align:right;\">Subtotal</th>\n")
        sb.append("</tr>\n")

        data.visibleItems.forEachIndexed { index, item ->
            val bg = if (index % 2 == 0) "#ffffff" else "#f8fafc"
            val typeLabel = if (item.type == CurrencyType.NOTE) "Note" else "Coin"
            sb.append("<tr style=\"background:$bg;border-bottom:1px solid #e2e8f0;\">\n")
            sb.append("<td style=\"padding:8px 12px;font-weight:600;\">${escapeHtml(item.label)}</td>\n")
            sb.append("<td style=\"padding:8px 12px;text-align:center;color:#64748b;\">$typeLabel</td>\n")
            sb.append("<td style=\"padding:8px 12px;text-align:right;font-weight:600;\">× ${item.quantity}</td>\n")
            sb.append("<td style=\"padding:8px 12px;text-align:right;font-weight:700;color:#0f172a;\">${escapeHtml(item.formattedSubtotal)}</td>\n")
            sb.append("</tr>\n")
        }
        sb.append("</table>\n")

        sb.append("</td></tr>\n")

        // Footer block
        sb.append("<tr><td style=\"background:#f8fafc;padding:16px 28px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#94a3b8;\">\n")
        sb.append("Generated with <strong>CashQ</strong> by SuBZeRo · Offline-First Cash Counter\n")
        sb.append("</td></tr>\n")

        sb.append("</table>\n") // end card table
        sb.append("</td></tr>\n")
        sb.append("</table>\n") // end outer table

        sb.append("</body>\n</html>")
        return sb.toString()
    }
}
