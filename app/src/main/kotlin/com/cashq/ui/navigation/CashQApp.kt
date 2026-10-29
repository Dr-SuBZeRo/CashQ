package com.cashq.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

/**
 * Root composable for CashQ. Hosts a [ModalNavigationDrawer] and switches
 * between Counter, History, and Settings screens. Bottom navigation bar has been removed.
 */
@Composable
fun CashQApp() {
    var selectedDestination by rememberSaveable { mutableStateOf(CashQDestination.COUNTER) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    // Back button dismisses the drawer if it is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CashQDrawerSheet(
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    scope.launch { drawerState.close() }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        when (selectedDestination) {
            CashQDestination.COUNTER -> CounterScreen(
                viewModel = counterViewModel,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier.fillMaxSize()
            )

            CashQDestination.HISTORY -> HistoryScreen(
                viewModel = historyViewModel,
                onLoadSessionIntoCounter = { session ->
                    counterViewModel.loadSession(session)
                    selectedDestination = CashQDestination.COUNTER
                },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier.fillMaxSize()
            )

            CashQDestination.SETTINGS -> SettingsScreen(
                viewModel = settingsViewModel,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
