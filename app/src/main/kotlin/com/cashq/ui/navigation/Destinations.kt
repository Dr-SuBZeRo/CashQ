package com.cashq.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation destinations for CashQ.
 * Uses type-safe navigation with kotlinx.serialization.
 */
@Serializable
data object CounterDestination

@Serializable
data object HistoryDestination

@Serializable
data object SettingsDestination
