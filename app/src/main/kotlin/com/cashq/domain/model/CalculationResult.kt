package com.cashq.domain.model

/**
 * Immutable result of a cash counting calculation.
 */
data class CalculationResult(
    val denominationSubtotals: Map<String, Long>, // denomination id -> subtotal in paise
    val denominationUnits: Map<String, Int>,       // denomination id -> quantity
    val activeTotalPaise: Long,
    val activeTotalRupees: Long,
    val activeTotalUnits: Long,
    val noteTotalPaise: Long,
    val coinTotalPaise: Long,
    val noteUnits: Long,
    val coinUnits: Long,
    val isNotesOnly: Boolean,
    val hasOverflow: Boolean = false
) {
    companion object {
        val ZERO = CalculationResult(
            denominationSubtotals = emptyMap(),
            denominationUnits = emptyMap(),
            activeTotalPaise = 0L,
            activeTotalRupees = 0L,
            activeTotalUnits = 0L,
            noteTotalPaise = 0L,
            coinTotalPaise = 0L,
            noteUnits = 0L,
            coinUnits = 0L,
            isNotesOnly = false,
            hasOverflow = false
        )
    }
}
