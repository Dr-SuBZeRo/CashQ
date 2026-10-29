package com.cashq.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cashq.CashQApplication
import com.cashq.ui.counter.CounterScreen
import com.cashq.ui.counter.CounterViewModel
import com.cashq.ui.counter.CounterViewModelFactory
import com.cashq.ui.history.HistoryScreen
import com.cashq.ui.history.HistoryViewModel
import com.cashq.ui.history.HistoryViewModelFactory
import com.cashq.ui.settings.SettingsScreen
import com.cashq.ui.settings.SettingsViewModel
import com.cashq.ui.settings.SettingsViewModelFactory

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val index: Int
)

private val bottomNavItems = listOf(
    BottomNavItem("Counter", Icons.Filled.Calculate, 0),
    BottomNavItem("History", Icons.Filled.History, 1),
    BottomNavItem("Settings", Icons.Filled.Settings, 2)
)

/**
 * Root composable for CashQ. Hosts a [Scaffold] with Material 3 bottom navigation
 * and switches between Counter, History, and Settings screens.
 */
@Composable
fun CashQApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val app = context.applicationContext as CashQApplication

    val counterViewModel: CounterViewModel = viewModel(
        factory = CounterViewModelFactory(app.sessionRepository, app.preferencesManager)
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(app.sessionRepository)
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(app.preferencesManager)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == item.index,
                        onClick = { selectedTab = item.index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> CounterScreen(
                viewModel = counterViewModel,
                modifier = Modifier.padding(innerPadding)
            )

            1 -> HistoryScreen(
                viewModel = historyViewModel,
                onLoadSessionIntoCounter = { session ->
                    counterViewModel.loadSession(session)
                    selectedTab = 0
                },
                modifier = Modifier.padding(innerPadding)
            )

            2 -> SettingsScreen(
                viewModel = settingsViewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
