package com.cashq.core.sharing

import com.cashq.domain.model.CoinCountingMode

/**
 * Legacy facade for generating plain-text summaries, delegating to the unified [CashCountShareData]
 * and [RichTextShareFormatter] engine.
 */
object ShareSummaryGenerator {

    /**
     * Generates a plain-text summary from session parameters.
     */
    fun generateSummary(
        title: String,
        timestamp: Long,
        quantities: Map<String, Int>,
        coinsVisible: Boolean,
        coinMode: CoinCountingMode
    ): String {
        val shareData = CashCountShareData.fromCounter(
            title = title,
            timestamp = timestamp,
            quantities = quantities,
            coinsVisible = coinsVisible,
            coinMode = coinMode
        )
        return RichTextShareFormatter.format(shareData)
    }
}
