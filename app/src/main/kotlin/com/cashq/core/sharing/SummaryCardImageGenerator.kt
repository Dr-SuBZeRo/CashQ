package com.cashq.core.sharing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.cashq.domain.model.CoinCountingMode

/**
 * Renders a high-resolution, unclipped summary card receipt image in Light or Dark theme.
 */
object SummaryCardImageGenerator {

    enum class CardTheme {
        LIGHT,
        DARK
    }

    private data class Palette(
        val bg: Int,
        val cardBg: Int,
        val cardBorder: Int,
        val title: Int,
        val subtitle: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val mutedText: Int,
        val divider: Int,
        val totalBg: Int,
        val totalBorder: Int,
        val totalLabel: Int,
        val totalValue: Int,
        val badgeBg: Int,
        val badgeText: Int,
        val rowEven: Int,
        val rowOdd: Int
    )

    private val LightPalette = Palette(
        bg = 0xFFF1F5F9.toInt(),
        cardBg = 0xFFFFFFFF.toInt(),
        cardBorder = 0xFFE2E8F0.toInt(),
        title = 0xFF0F172A.toInt(),
        subtitle = 0xFF64748B.toInt(),
        primaryText = 0xFF0F172A.toInt(),
        secondaryText = 0xFF334155.toInt(),
        mutedText = 0xFF64748B.toInt(),
        divider = 0xFFE2E8F0.toInt(),
        totalBg = 0xFFEFF6FF.toInt(),
        totalBorder = 0xFFBFDBFE.toInt(),
        totalLabel = 0xFF1E40AF.toInt(),
        totalValue = 0xFF1E3A8A.toInt(),
        badgeBg = 0xFFFEF3C7.toInt(),
        badgeText = 0xFF92400E.toInt(),
        rowEven = 0xFFFFFFFF.toInt(),
        rowOdd = 0xFFF8FAFC.toInt()
    )

    private val DarkPalette = Palette(
        bg = 0xFF090D16.toInt(),
        cardBg = 0xFF131B2E.toInt(),
        cardBorder = 0xFF243048.toInt(),
        title = 0xFFF8FAFC.toInt(),
        subtitle = 0xFF94A3B8.toInt(),
        primaryText = 0xFFF8FAFC.toInt(),
        secondaryText = 0xFFCBD5E1.toInt(),
        mutedText = 0xFF64748B.toInt(),
        divider = 0xFF243048.toInt(),
        totalBg = 0xFF18243C.toInt(),
        totalBorder = 0xFF2E456B.toInt(),
        totalLabel = 0xFF60A5FA.toInt(),
        totalValue = 0xFF93C5FD.toInt(),
        badgeBg = 0xFF451A03.toInt(),
        badgeText = 0xFFFBBF24.toInt(),
        rowEven = 0xFF131B2E.toInt(),
        rowOdd = 0xFF192238.toInt()
    )

    /**
     * Measures and renders the standalone summary card bitmap.
     *
     * @param data Single immutable share-data model.
     * @param theme Light or Dark card aesthetic.
     * @param logo Optional CashQ logo bitmap (72x72 drawn in header if provided).
     * @return Generated high-resolution Bitmap.
     */
    fun generateCard(
        data: CashCountShareData,
        theme: CardTheme = CardTheme.LIGHT,
        logo: Bitmap? = null
    ): Bitmap {
        val palette = if (theme == CardTheme.LIGHT) LightPalette else DarkPalette

        val width = 1080
        val margin = 48f
        val cardWidth = width - (margin * 2)

        val items = if (data.visibleItems.any { it.quantity > 0 }) {
            data.visibleItems.filter { it.quantity > 0 }
        } else {
            data.visibleItems
        }

        // Layout metrics calculation
        val rowHeight = 64f
        val headerHeight = 200f
        val totalBoxHeight = 230f
        val breakdownHeaderHeight = 60f
        val rowsTotalHeight = items.size * rowHeight
        val footerHeight = 90f
        val innerPadding = 40f

        val cardHeight = innerPadding + headerHeight + 20f + totalBoxHeight + 24f +
                breakdownHeaderHeight + rowsTotalHeight + 20f + footerHeight + innerPadding
        val totalHeight = (cardHeight + (margin * 2)).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer canvas background
        val bgPaint = Paint().apply { color = palette.bg }
        canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

        // Card container with rounded corners and border
        val cardRect = RectF(margin, margin, margin + cardWidth, margin + cardHeight)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.cardBg }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.cardBorder
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        var currentY = margin + innerPadding
        val leftX = margin + innerPadding
        val rightX = margin + cardWidth - innerPadding
        val contentWidth = rightX - leftX

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header (Logo, App Name, Session Title, Date)
        val logoSize = 64f
        if (logo != null && !logo.isRecycled) {
            val logoRect = RectF(leftX, currentY, leftX + logoSize, currentY + logoSize)
            canvas.drawBitmap(logo, null, logoRect, null)
        } else {
            // Branded circle fallback
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.totalBorder }
            canvas.drawCircle(leftX + (logoSize / 2), currentY + (logoSize / 2), logoSize / 2, badgePaint)
            textPaint.apply {
                color = palette.totalLabel
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("₹", leftX + (logoSize / 2), currentY + (logoSize / 2) + 12f, textPaint)
        }

        val textStartX = leftX + logoSize + 20f

        // App Name
        textPaint.apply {
            color = palette.title
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(data.appName, textStartX, currentY + 36f, textPaint)

        // App Subtitle
        textPaint.apply {
            color = palette.subtitle
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(data.appSubtitle, textStartX, currentY + 66f, textPaint)

        currentY += logoSize + 24f

        // Session Title
        textPaint.apply {
            color = palette.title
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.title, leftX, currentY + 28f, textPaint)
        currentY += 40f

        // Date and Mode Badge
        textPaint.apply {
            color = palette.mutedText
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("📅 ${data.formattedDateTime}", leftX, currentY + 24f, textPaint)

        if (data.isNotesOnly || data.coinMode == CoinCountingMode.COMBINED) {
            val modeText = if (data.isNotesOnly) "Notes Only" else "Combined ₹20/₹10"
            val badgeWidth = textPaint.measureText(modeText) + 32f
            val badgeHeight = 36f
            val badgeRect = RectF(rightX - badgeWidth, currentY, rightX, currentY + badgeHeight)
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.badgeBg }
            canvas.drawRoundRect(badgeRect, 8f, 8f, badgeBgPaint)

            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.badgeText
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(modeText, badgeRect.centerX(), badgeRect.centerY() + 7f, badgeTextPaint)
        }

        currentY += 44f

        // 2. Grand Total Card Box
        val totalBoxRect = RectF(leftX, currentY, rightX, currentY + totalBoxHeight)
        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.totalBg }
        canvas.drawRoundRect(totalBoxRect, 20f, 20f, totalBgPaint)

        val totalBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.totalBorder
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(totalBoxRect, 20f, 20f, totalBorderPaint)

        // Total Label
        textPaint.apply {
            color = palette.totalLabel
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.totalLabel, totalBoxRect.centerX(), currentY + 44f, textPaint)

        // Amount
        textPaint.apply {
            color = palette.totalValue
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.formattedTotalRupees, totalBoxRect.centerX(), currentY + 116f, textPaint)

        // Units
        textPaint.apply {
            color = palette.secondaryText
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("📦 ${data.formattedTotalUnits} units counted", totalBoxRect.centerX(), currentY + 160f, textPaint)

        // Subtotals line
        textPaint.apply {
            color = palette.mutedText
            textSize = 20f
        }
        val subtotalSummary = if (!data.isNotesOnly && data.coinsVisible && data.coinTotalRupees > 0) {
            "Notes: ${data.formattedNoteTotalRupees} (${data.formattedNoteUnits})  •  Coins: ${data.formattedCoinTotalRupees} (${data.formattedCoinUnits})"
        } else {
            "Notes Subtotal: ${data.formattedNoteTotalRupees} (${data.formattedNoteUnits} units)"
        }
        canvas.drawText(subtotalSummary, totalBoxRect.centerX(), currentY + 200f, textPaint)

        currentY += totalBoxHeight + 24f

        // 3. Denomination Breakdown Header
        textPaint.apply {
            color = palette.secondaryText
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("DENOMINATION", leftX + 16f, currentY + 28f, textPaint)

        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("COUNT", leftX + (contentWidth * 0.55f), currentY + 28f, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("SUBTOTAL", rightX - 16f, currentY + 28f, textPaint)

        currentY += 42f

        // Divider under header
        val dividerPaint = Paint().apply {
            color = palette.divider
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)
        currentY += 8f

        // Rows
        val rowBgPaint = Paint()
        items.forEachIndexed { index, item ->
            val rowY = currentY + (index * rowHeight)
            val rowRect = RectF(leftX, rowY, rightX, rowY + rowHeight - 6f)
            rowBgPaint.color = if (index % 2 == 0) palette.rowEven else palette.rowOdd
            canvas.drawRoundRect(rowRect, 8f, 8f, rowBgPaint)

            // Label
            textPaint.apply {
                color = palette.primaryText
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(item.label, leftX + 16f, rowY + 38f, textPaint)

            // Quantity
            textPaint.apply {
                color = palette.secondaryText
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("× ${item.quantity}", leftX + (contentWidth * 0.55f), rowY + 38f, textPaint)

            // Subtotal
            textPaint.apply {
                color = palette.primaryText
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(item.formattedSubtotal, rightX - 16f, rowY + 38f, textPaint)
        }

        currentY += rowsTotalHeight + 16f

        // 4. Footer
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)
        currentY += 28f

        textPaint.apply {
            color = palette.mutedText
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Created with CashQ by SuBZeRo  •  100% Offline & Private", totalBoxRect.centerX(), currentY + 20f, textPaint)

        return bitmap
    }
}
