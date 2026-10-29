package com.cashq.domain.calculation

import com.cashq.domain.model.CalculationResult
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination

/**
 * Pure Kotlin calculation engine for Indian currency denominations.
 * Independent of Android UI, Compose, Room, and network dependencies.
 * Uses exact Long arithmetic (in paise) to guarantee precision.
 */
object CashCalculationEngine {

    /**
     * Calculates subtotals, active totals, unit counts, and category breakdowns.
     *
     * @param quantities Map of denomination ID to entered quantity (non-negative integer).
     * @param coinsVisible Whether coin rows are active and visible.
     * @param coinMode SEPARATE or COMBINED counting mode for ₹20 and ₹10.
     * @return [CalculationResult] containing all computed totals and breakdowns.
     */
    fun calculate(
        quantities: Map<String, Int>,
        coinsVisible: Boolean,
        coinMode: CoinCountingMode
    ): CalculationResult {
        val visibleDenominations = getVisibleDenominations(coinsVisible, coinMode)
        val subtotals = mutableMapOf<String, Long>()
        val unitsMap = mutableMapOf<String, Int>()

        var activeTotalPaise = 0L
        var activeUnits = 0L
        var noteTotalPaise = 0L
        var coinTotalPaise = 0L
        var noteUnits = 0L
        var coinUnits = 0L
        var hasOverflow = false

        for (denom in visibleDenominations) {
            val qty = (quantities[denom.id] ?: 0).coerceAtLeast(0)
            unitsMap[denom.id] = qty

            try {
                val subtotalPaise = Math.multiplyExact(denom.valueInPaise, qty.toLong())
                subtotals[denom.id] = subtotalPaise

                activeTotalPaise = Math.addExact(activeTotalPaise, subtotalPaise)
                activeUnits = Math.addExact(activeUnits, qty.toLong())

                if (denom.type == CurrencyType.NOTE) {
                    noteTotalPaise = Math.addExact(noteTotalPaise, subtotalPaise)
                    noteUnits = Math.addExact(noteUnits, qty.toLong())
                } else {
                    coinTotalPaise = Math.addExact(coinTotalPaise, subtotalPaise)
                    coinUnits = Math.addExact(coinUnits, qty.toLong())
                }
            } catch (_: ArithmeticException) {
                hasOverflow = true
            }
        }

        return CalculationResult(
            denominationSubtotals = subtotals,
            denominationUnits = unitsMap,
            activeTotalPaise = activeTotalPaise,
            activeTotalRupees = activeTotalPaise / 100L,
            activeTotalUnits = activeUnits,
            noteTotalPaise = noteTotalPaise,
            coinTotalPaise = coinTotalPaise,
            noteUnits = noteUnits,
            coinUnits = coinUnits,
            isNotesOnly = !coinsVisible,
            hasOverflow = hasOverflow
        )
    }

    /**
     * Returns the ordered list of denominations to display based on visibility and counting mode.
     */
    fun getVisibleDenominations(
        coinsVisible: Boolean,
        coinMode: CoinCountingMode
    ): List<Denomination> {
        val notes = if (coinMode == CoinCountingMode.COMBINED) {
            // When combined, ₹20 and ₹10 are merged
            listOf(
                Denomination.NOTE_500,
                Denomination.NOTE_200,
                Denomination.NOTE_100,
                Denomination.NOTE_50,
                Denomination.COMBINED_20,
                Denomination.COMBINED_10
            )
        } else {
            Denomination.ALL_NOTES
        }

        if (!coinsVisible) {
            return notes
        }

        val coins = if (coinMode == CoinCountingMode.COMBINED) {
            // In combined mode, ₹20 and ₹10 coins are merged into the notes list above
            listOf(
                Denomination.COIN_5,
                Denomination.COIN_2,
                Denomination.COIN_1
            )
        } else {
            Denomination.ALL_COINS
        }

        return notes + coins
    }

    /**
     * Converts quantities from SEPARATE mode to COMBINED mode.
     * Merges note_20 + coin_20 -> combined_20, and note_10 + coin_10 -> combined_10.
     * Preserves monetary value and all other denominations.
     */
    fun convertSeparateToCombined(currentQuantities: Map<String, Int>): Map<String, Int> {
        val result = currentQuantities.toMutableMap()

        val note20 = result.remove(Denomination.NOTE_20.id) ?: 0
        val coin20 = result.remove(Denomination.COIN_20.id) ?: 0
        result[Denomination.COMBINED_20.id] = note20 + coin20

        val note10 = result.remove(Denomination.NOTE_10.id) ?: 0
        val coin10 = result.remove(Denomination.COIN_10.id) ?: 0
        result[Denomination.COMBINED_10.id] = note10 + coin10

        return result
    }

    /**
     * Converts quantities from COMBINED mode to SEPARATE mode.
     * Assigns combined_20 -> note_20 (coin_20 = 0), and combined_10 -> note_10 (coin_10 = 0).
     * Preserves total monetary value and informs the user.
     */
    fun convertCombinedToSeparate(currentQuantities: Map<String, Int>): Map<String, Int> {
        val result = currentQuantities.toMutableMap()

        val combined20 = result.remove(Denomination.COMBINED_20.id) ?: 0
        result[Denomination.NOTE_20.id] = combined20
        result[Denomination.COIN_20.id] = 0

        val combined10 = result.remove(Denomination.COMBINED_10.id) ?: 0
        result[Denomination.NOTE_10.id] = combined10
        result[Denomination.COIN_10.id] = 0

        return result
    }
}
