package com.cashq.core.sharing

import com.cashq.core.formatting.CurrencyFormatter
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.domain.calculation.CashCalculationEngine
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Individual denomination row formatted for sharing.
 */
data class DenominationShareItem(
    val id: String,
    val label: String,
    val shortLabel: String,
    val type: CurrencyType,
    val faceValueRupees: Long,
    val quantity: Int,
    val subtotalPaise: Long,
    val subtotalRupees: Long,
    val formattedSubtotal: String
)

/**
 * Single immutable source of truth for all CashQ share formats (Rich Text, Image, Email).
 */
data class CashCountShareData(
    val title: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val isNotesOnly: Boolean,
    val coinsVisible: Boolean,
    val coinMode: CoinCountingMode,
    val totalPaise: Long,
    val totalRupees: Long,
    val formattedTotalRupees: String,
    val totalUnits: Long,
    val formattedTotalUnits: String,
    val noteTotalPaise: Long,
    val noteTotalRupees: Long,
    val formattedNoteTotalRupees: String,
    val noteUnits: Long,
    val formattedNoteUnits: String,
    val coinTotalPaise: Long,
    val coinTotalRupees: Long,
    val formattedCoinTotalRupees: String,
    val coinUnits: Long,
    val formattedCoinUnits: String,
    val visibleItems: List<DenominationShareItem>,
    val noteItems: List<DenominationShareItem>,
    val coinItems: List<DenominationShareItem>,
    val appName: String = "CashQ",
    val appSubtitle: String = "Indian Rupee Cash Counter",
    val developerAttribution: String = "SuBZeRo"
) {
    val modeLabel: String
        get() = when {
            isNotesOnly || !coinsVisible -> "Banknotes Only (Coins Hidden)"
            coinMode == CoinCountingMode.COMBINED -> "Combined ₹20 & ₹10 Denominations"
            else -> "Separate Denominations"
        }

    val totalLabel: String
        get() = if (isNotesOnly || !coinsVisible) "TOTAL CASH (Notes Only)" else "TOTAL CASH"

    companion object {
        private const val DATE_PATTERN = "dd MMMM yyyy, hh:mm a"

        /**
         * Factory function to create share data from current live Counter state.
         */
        fun fromCounter(
            title: String?,
            timestamp: Long = System.currentTimeMillis(),
            quantities: Map<String, Int>,
            coinsVisible: Boolean,
            coinMode: CoinCountingMode
        ): CashCountShareData {
            val calc = CashCalculationEngine.calculate(quantities, coinsVisible, coinMode)
            val visibleDenoms = CashCalculationEngine.getVisibleDenominations(coinsVisible, coinMode)
            val isNotesOnly = !coinsVisible || calc.isNotesOnly

            return buildShareData(
                title = if (title.isNullOrBlank()) "Cash Count Summary" else title.trim(),
                timestamp = timestamp,
                isNotesOnly = isNotesOnly,
                coinsVisible = coinsVisible,
                coinMode = coinMode,
                quantities = quantities,
                visibleDenoms = visibleDenoms,
                totalPaise = calc.activeTotalPaise,
                totalRupees = calc.activeTotalRupees,
                totalUnits = calc.activeTotalUnits,
                noteTotalPaise = calc.noteTotalPaise,
                noteUnits = calc.noteUnits,
                coinTotalPaise = calc.coinTotalPaise,
                coinUnits = calc.coinUnits
            )
        }

        /**
         * Factory function to create share data from a saved Room history entity.
         */
        fun fromSavedSession(session: SavedSessionEntity): CashCountShareData {
            val quantities = session.getQuantitiesMap()
            val coinMode = try {
                CoinCountingMode.valueOf(session.coinMode)
            } catch (_: Exception) {
                CoinCountingMode.SEPARATE
            }
            val coinsVisible = session.coinsVisible
            val calc = CashCalculationEngine.calculate(quantities, coinsVisible, coinMode)
            val visibleDenoms = CashCalculationEngine.getVisibleDenominations(coinsVisible, coinMode)
            val isNotesOnly = session.isNotesOnly || !coinsVisible || calc.isNotesOnly

            return buildShareData(
                title = session.title.ifBlank { "Cash Count Summary" },
                timestamp = session.createdAt,
                isNotesOnly = isNotesOnly,
                coinsVisible = coinsVisible,
                coinMode = coinMode,
                quantities = quantities,
                visibleDenoms = visibleDenoms,
                totalPaise = session.totalPaise,
                totalRupees = session.totalPaise / 100L,
                totalUnits = session.totalUnits,
                noteTotalPaise = calc.noteTotalPaise,
                noteUnits = calc.noteUnits,
                coinTotalPaise = if (isNotesOnly) 0L else calc.coinTotalPaise,
                coinUnits = if (isNotesOnly) 0L else calc.coinUnits
            )
        }

        private fun buildShareData(
            title: String,
            timestamp: Long,
            isNotesOnly: Boolean,
            coinsVisible: Boolean,
            coinMode: CoinCountingMode,
            quantities: Map<String, Int>,
            visibleDenoms: List<Denomination>,
            totalPaise: Long,
            totalRupees: Long,
            totalUnits: Long,
            noteTotalPaise: Long,
            noteUnits: Long,
            coinTotalPaise: Long,
            coinUnits: Long
        ): CashCountShareData {
            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.forLanguageTag("en-IN"))
            val formattedDate = sdf.format(Date(timestamp))

            val visibleItems = visibleDenoms.map { denom ->
                val qty = (quantities[denom.id] ?: 0).coerceAtLeast(0)
                val subtotalPaise = denom.valueInPaise * qty.toLong()
                val subtotalRupees = subtotalPaise / 100L
                DenominationShareItem(
                    id = denom.id,
                    label = denom.label,
                    shortLabel = denom.shortLabel,
                    type = denom.type,
                    faceValueRupees = denom.valueInRupees,
                    quantity = qty,
                    subtotalPaise = subtotalPaise,
                    subtotalRupees = subtotalRupees,
                    formattedSubtotal = CurrencyFormatter.formatRupees(subtotalRupees)
                )
            }

            val noteItems = visibleItems.filter { it.type == CurrencyType.NOTE }
            val coinItems = if (isNotesOnly || !coinsVisible) emptyList() else visibleItems.filter { it.type == CurrencyType.COIN }

            val effectiveCoinPaise = if (isNotesOnly || !coinsVisible) 0L else coinTotalPaise
            val effectiveCoinUnits = if (isNotesOnly || !coinsVisible) 0L else coinUnits

            return CashCountShareData(
                title = title,
                timestamp = timestamp,
                formattedDateTime = formattedDate,
                isNotesOnly = isNotesOnly,
                coinsVisible = coinsVisible && !isNotesOnly,
                coinMode = coinMode,
                totalPaise = totalPaise,
                totalRupees = totalRupees,
                formattedTotalRupees = CurrencyFormatter.formatRupees(totalRupees),
                totalUnits = totalUnits,
                formattedTotalUnits = CurrencyFormatter.formatUnits(totalUnits),
                noteTotalPaise = noteTotalPaise,
                noteTotalRupees = noteTotalPaise / 100L,
                formattedNoteTotalRupees = CurrencyFormatter.formatRupees(noteTotalPaise / 100L),
                noteUnits = noteUnits,
                formattedNoteUnits = CurrencyFormatter.formatUnits(noteUnits),
                coinTotalPaise = effectiveCoinPaise,
                coinTotalRupees = effectiveCoinPaise / 100L,
                formattedCoinTotalRupees = CurrencyFormatter.formatRupees(effectiveCoinPaise / 100L),
                coinUnits = effectiveCoinUnits,
                formattedCoinUnits = CurrencyFormatter.formatUnits(effectiveCoinUnits),
                visibleItems = visibleItems,
                noteItems = noteItems,
                coinItems = coinItems
            )
        }
    }
}
