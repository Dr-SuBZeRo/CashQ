package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType

/**
 * Generates email-safe, responsive HTML tables and subjects for [CashCountShareData].
 * Formats cash counts as professional, branded financial reports.
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
     * Renders a complete, standalone, responsive HTML financial report with inline CSS.
     */
    fun formatHtml(data: CashCountShareData): String {
        val safeTitle = escapeHtml(data.title.ifBlank { "Cash Count Summary" })
        val safeDate = escapeHtml(data.formattedDateTime)

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n")
        sb.append("<html lang=\"en\">\n")
        sb.append("<head>\n")
        sb.append("<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>${escapeHtml(data.title)}</title>\n")
        sb.append("</head>\n")
        sb.append("<body style=\"margin:0;padding:20px;background-color:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;\">\n")

        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n")
        sb.append("<tr><td align=\"center\">\n")

        // Card container
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"max-width:580px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.06);border:1px solid #cbd5e1;\" cellpadding=\"0\" cellspacing=\"0\">\n")

        // Branded Email Header Block
        sb.append("<tr><td style=\"background:#0f172a;padding:24px 28px;text-align:left;border-bottom:3px solid #2563eb;\">\n")
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n")
        sb.append("<tr>\n")
        sb.append("<td style=\"vertical-align:middle;\">\n")
        // Graceful stylized text/brand emblem
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"display:inline-table;vertical-align:middle;margin-right:12px;\">\n")
        sb.append("<tr><td style=\"width:38px;height:38px;background:#2563eb;border-radius:8px;text-align:center;vertical-align:middle;color:#ffffff;font-size:20px;font-weight:800;font-family:-apple-system,BlinkMacSystemFont,sans-serif;\">₹</td></tr>\n")
        sb.append("</table>\n")
        sb.append("<div style=\"display:inline-block;vertical-align:middle;\">\n")
        sb.append("<div style=\"font-size:22px;font-weight:800;color:#ffffff;letter-spacing:0.5px;line-height:1.2;\">CashQ</div>\n")
        sb.append("<div style=\"font-size:12px;color:#94a3b8;font-weight:400;margin-top:2px;\">Indian Rupee Cash Counter</div>\n")
        sb.append("</div>\n")
        sb.append("</td>\n")
        sb.append("<td align=\"right\" style=\"vertical-align:middle;\">\n")
        sb.append("<span style=\"display:inline-block;background:#1e293b;color:#38bdf8;border:1px solid #0284c7;padding:5px 12px;border-radius:14px;font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;\">CASH COUNT REPORT</span>\n")
        sb.append("</td>\n")
        sb.append("</tr>\n")
        sb.append("</table>\n")
        sb.append("</td></tr>\n")

        // Session Information Block
        sb.append("<tr><td style=\"padding:24px 28px 16px 28px;\">\n")
        sb.append("<h2 style=\"margin:0 0 6px 0;font-size:18px;color:#0f172a;font-weight:700;word-break:break-word;overflow-wrap:break-word;\">$safeTitle</h2>\n")
        sb.append("<p style=\"margin:0 0 14px 0;font-size:13px;color:#64748b;\">📅 $safeDate</p>\n")

        // Mode Badge
        when {
            data.isNotesOnly || !data.coinsVisible -> {
                sb.append("<div style=\"display:inline-block;background:#fef3c7;color:#92400e;border:1px solid #fde68a;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;margin-bottom:18px;\">Mode: Banknotes Only (Coins Hidden)</div>\n")
            }
            data.coinMode == CoinCountingMode.COMBINED -> {
                sb.append("<div style=\"display:inline-block;background:#e0f2fe;color:#0369a1;border:1px solid #bae6fd;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;margin-bottom:18px;\">Mode: Combined ₹20 &amp; ₹10 Denominations</div>\n")
            }
            else -> {
                sb.append("<div style=\"display:inline-block;background:#f1f5f9;color:#475569;border:1px solid #cbd5e1;padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;margin-bottom:18px;\">Mode: Separate Denominations</div>\n")
            }
        }

        // Prominent Total Summary Panel
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"background:#f0f7ff;border-radius:10px;padding:20px;border:1px solid #bfdbfe;margin-bottom:20px;\" cellpadding=\"0\" cellspacing=\"0\">\n")
        sb.append("<tr><td align=\"center\" style=\"padding:4px 8px 12px 8px;\">\n")
        sb.append("<div style=\"font-size:12px;text-transform:uppercase;color:#1e40af;font-weight:700;letter-spacing:1px;margin-bottom:6px;\">${escapeHtml(data.totalLabel)}</div>\n")
        sb.append("<div style=\"font-size:34px;font-weight:800;color:#1e3a8a;letter-spacing:-0.5px;\">${escapeHtml(data.formattedTotalRupees)}</div>\n")
        sb.append("<div style=\"font-size:14px;color:#2563eb;font-weight:600;margin-top:6px;\">📦 ${escapeHtml(data.formattedTotalUnits)} units counted</div>\n")
        sb.append("</td></tr>\n")

        // Subtotals divider inside panel
        sb.append("<tr><td style=\"border-top:1px solid #dbeafe;padding-top:12px;\">\n")
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"font-size:13px;color:#1e3a8a;\">\n")
        sb.append("<tr>\n")
        sb.append("<td style=\"padding:4px 0;\"><strong>💵 Banknotes Subtotal:</strong></td>\n")
        sb.append("<td align=\"right\" style=\"padding:4px 0;font-weight:700;\">${escapeHtml(data.formattedNoteTotalRupees)} <span style=\"font-weight:400;color:#3b82f6;\">(${escapeHtml(data.formattedNoteUnits)} units)</span></td>\n")
        sb.append("</tr>\n")

        if (data.coinsVisible && !data.isNotesOnly && data.coinItems.isNotEmpty()) {
            sb.append("<tr>\n")
            sb.append("<td style=\"padding:4px 0;\"><strong>🪙 Coins Subtotal:</strong></td>\n")
            sb.append("<td align=\"right\" style=\"padding:4px 0;font-weight:700;\">${escapeHtml(data.formattedCoinTotalRupees)} <span style=\"font-weight:400;color:#3b82f6;\">(${escapeHtml(data.formattedCoinUnits)} units)</span></td>\n")
            sb.append("</tr>\n")
        } else if (data.isNotesOnly || !data.coinsVisible) {
            sb.append("<tr>\n")
            sb.append("<td colspan=\"2\" style=\"padding:6px 0 2px 0;text-align:center;\">\n")
            sb.append("<span style=\"display:inline-block;background:#fef3c7;color:#92400e;border:1px solid #fde68a;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:700;\">NOTES ONLY · Coins excluded from count</span>\n")
            sb.append("</td>\n")
            sb.append("</tr>\n")
        }
        sb.append("</table>\n")
        sb.append("</td></tr>\n")
        sb.append("</table>\n")

        // Denomination Breakdown Table
        sb.append("<h3 style=\"margin:0 0 10px 0;font-size:14px;text-transform:uppercase;color:#334155;letter-spacing:0.5px;\">Denomination Breakdown</h3>\n")
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;font-size:13px;margin-bottom:24px;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;\">\n")

        // 3-Column Table Header
        sb.append("<tr style=\"background:#0f172a;color:#ffffff;text-align:left;font-weight:600;font-size:12px;text-transform:uppercase;letter-spacing:0.5px;\">\n")
        sb.append("<th style=\"padding:10px 14px;\">Denomination</th>\n")
        sb.append("<th style=\"padding:10px 14px;text-align:right;\">Count</th>\n")
        sb.append("<th style=\"padding:10px 14px;text-align:right;\">Subtotal</th>\n")
        sb.append("</tr>\n")

        // Rows depending on mode
        var rowIndex = 0
        fun appendItemRow(item: DenominationShareItem) {
            val bg = if (rowIndex % 2 == 0) "#ffffff" else "#f8fafc"
            sb.append("<tr style=\"background:$bg;border-bottom:1px solid #e2e8f0;\">\n")
            sb.append("<td style=\"padding:10px 14px;font-weight:600;color:#0f172a;\">${escapeHtml(item.label)}</td>\n")
            sb.append("<td style=\"padding:10px 14px;text-align:right;font-weight:600;color:#475569;\">× ${item.quantity}</td>\n")
            sb.append("<td style=\"padding:10px 14px;text-align:right;font-weight:700;color:#0f172a;\">${escapeHtml(item.formattedSubtotal)}</td>\n")
            sb.append("</tr>\n")
            rowIndex++
        }

        if (data.coinMode == CoinCountingMode.SEPARATE) {
            // Banknotes Section Heading
            sb.append("<tr style=\"background:#f1f5f9;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;\">\n")
            sb.append("<th colspan=\"3\" style=\"padding:8px 14px;text-align:left;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.8px;\">💵 Banknotes</th>\n")
            sb.append("</tr>\n")
            data.noteItems.forEach { appendItemRow(it) }

            // Coins Section Heading (if coins visible and not notes-only)
            if (data.coinsVisible && !data.isNotesOnly && data.coinItems.isNotEmpty()) {
                sb.append("<tr style=\"background:#f1f5f9;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;\">\n")
                sb.append("<th colspan=\"3\" style=\"padding:8px 14px;text-align:left;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.8px;\">🪙 Coins</th>\n")
                sb.append("</tr>\n")
                data.coinItems.forEach { appendItemRow(it) }
            }
        } else {
            // Combined Mode: Unified list distinguishing banknotes & combined from coins
            sb.append("<tr style=\"background:#f1f5f9;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;\">\n")
            sb.append("<th colspan=\"3\" style=\"padding:8px 14px;text-align:left;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.8px;\">💵 Banknotes &amp; Combined Denominations</th>\n")
            sb.append("</tr>\n")
            data.noteItems.forEach { appendItemRow(it) }

            if (data.coinsVisible && !data.isNotesOnly && data.coinItems.isNotEmpty()) {
                sb.append("<tr style=\"background:#f1f5f9;border-top:1px solid #e2e8f0;border-bottom:1px solid #e2e8f0;\">\n")
                sb.append("<th colspan=\"3\" style=\"padding:8px 14px;text-align:left;font-size:11px;font-weight:700;color:#475569;text-transform:uppercase;letter-spacing:0.8px;\">🪙 Coins (₹5, ₹2, ₹1)</th>\n")
                sb.append("</tr>\n")
                data.coinItems.forEach { appendItemRow(it) }
            }
        }

        sb.append("</table>\n")

        // Closing Summary Recap
        sb.append("<table role=\"presentation\" width=\"100%\" style=\"background:#f8fafc;border-radius:8px;padding:12px 16px;border:1px solid #e2e8f0;margin-bottom:20px;font-size:13px;\" cellpadding=\"0\" cellspacing=\"0\">\n")
        sb.append("<tr>\n")
        sb.append("<td style=\"padding:4px 0;font-weight:600;color:#0f172a;\">Final Total:</td>\n")
        sb.append("<td align=\"right\" style=\"padding:4px 0;font-weight:800;color:#1e3a8a;font-size:15px;\">${escapeHtml(data.formattedTotalRupees)} <span style=\"font-size:12px;font-weight:500;color:#64748b;\">(${escapeHtml(data.formattedTotalUnits)} units)</span></td>\n")
        sb.append("</tr>\n")
        sb.append("</table>\n")

        sb.append("</td></tr>\n")

        // Footer block
        sb.append("<tr><td style=\"background:#f8fafc;padding:18px 28px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#64748b;line-height:1.6;\">\n")
        sb.append("Created with <strong>CashQ</strong> by SuBZeRo<br>\n")
        sb.append("<span style=\"color:#059669;font-weight:600;\">🔒 100% Offline &amp; Private</span> · Native Android Cash Counter\n")
        sb.append("</td></tr>\n")

        sb.append("</table>\n") // end card table
        sb.append("</td></tr>\n")
        sb.append("</table>\n") // end outer table

        sb.append("</body>\n</html>")
        return sb.toString()
    }
}
