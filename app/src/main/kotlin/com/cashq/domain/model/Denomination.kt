package com.cashq.domain.model

/**
 * Distinguishes between paper currency notes and metallic coins.
 */
enum class CurrencyType {
    NOTE,
    COIN
}

/**
 * Mode for counting ₹20 and ₹10 denominations.
 */
enum class CoinCountingMode {
    /**
     * ₹20 note, ₹20 coin, ₹10 note, and ₹10 coin are tracked as distinct rows.
     */
    SEPARATE,

    /**
     * ₹20 notes and coins are merged into a single ₹20 row.
     * ₹10 notes and coins are merged into a single ₹10 row.
     */
    COMBINED
}

/**
 * Canonical denomination definition.
 * All monetary values are represented in paise (1 Rupee = 100 paise) to prevent floating-point inaccuracies.
 *
 * @param id Unique identifier used for persistence and state mapping
 * @param label Display label (e.g., "₹500", "₹20 Note", "₹20 Coin")
 * @param shortLabel Short denomination label (e.g. "₹500", "₹20")
 * @param valueInPaise Exact face value in paise (₹500 = 50,000 paise)
 * @param type NOTE or COIN
 * @param isCombinedEntry True if this is a merged ₹20 or ₹10 row in combined mode
 */
data class Denomination(
    val id: String,
    val label: String,
    val shortLabel: String,
    val valueInPaise: Long,
    val type: CurrencyType,
    val isCombinedEntry: Boolean = false
) {
    val valueInRupees: Long = valueInPaise / 100L

    companion object {
        // Banknotes
        val NOTE_500 = Denomination("note_500", "₹500 Note", "₹500", 50000L, CurrencyType.NOTE)
        val NOTE_200 = Denomination("note_200", "₹200 Note", "₹200", 20000L, CurrencyType.NOTE)
        val NOTE_100 = Denomination("note_100", "₹100 Note", "₹100", 10000L, CurrencyType.NOTE)
        val NOTE_50 = Denomination("note_50", "₹50 Note", "₹50", 5000L, CurrencyType.NOTE)
        val NOTE_20 = Denomination("note_20", "₹20 Note", "₹20", 2000L, CurrencyType.NOTE)
        val NOTE_10 = Denomination("note_10", "₹10 Note", "₹10", 1000L, CurrencyType.NOTE)

        // Coins
        val COIN_20 = Denomination("coin_20", "₹20 Coin", "₹20", 2000L, CurrencyType.COIN)
        val COIN_10 = Denomination("coin_10", "₹10 Coin", "₹10", 1000L, CurrencyType.COIN)
        val COIN_5 = Denomination("coin_5", "₹5 Coin", "₹5", 500L, CurrencyType.COIN)
        val COIN_2 = Denomination("coin_2", "₹2 Coin", "₹2", 200L, CurrencyType.COIN)
        val COIN_1 = Denomination("coin_1", "₹1 Coin", "₹1", 100L, CurrencyType.COIN)

        // Combined entries used when CoinCountingMode.COMBINED
        val COMBINED_20 = Denomination("combined_20", "₹20 (Note + Coin)", "₹20", 2000L, CurrencyType.NOTE, isCombinedEntry = true)
        val COMBINED_10 = Denomination("combined_10", "₹10 (Note + Coin)", "₹10", 1000L, CurrencyType.NOTE, isCombinedEntry = true)

        /**
         * The standard 11 canonical denominations of the Indian Rupee.
         * Single source of truth across the entire application.
         */
        val ALL_CANONICAL = listOf(
            NOTE_500, NOTE_200, NOTE_100, NOTE_50, NOTE_20, NOTE_10,
            COIN_20, COIN_10, COIN_5, COIN_2, COIN_1
        )

        val ALL_NOTES = listOf(NOTE_500, NOTE_200, NOTE_100, NOTE_50, NOTE_20, NOTE_10)
        val ALL_COINS = listOf(COIN_20, COIN_10, COIN_5, COIN_2, COIN_1)
    }
}
