package com.cashq.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Primary navigation drawer destinations for CashQ.
 */
enum class CashQDestination(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    COUNTER(
        label = "Counter",
        title = "CashQ",
        subtitle = "Indian Rupee Cash Counter",
        icon = Icons.Filled.Calculate
    ),
    HISTORY(
        label = "History",
        title = "CashQ",
        subtitle = "Count History",
        icon = Icons.Filled.History
    ),
    SETTINGS(
        label = "Settings",
        title = "CashQ",
        subtitle = "Settings & Preferences",
        icon = Icons.Filled.Settings
    )
}
