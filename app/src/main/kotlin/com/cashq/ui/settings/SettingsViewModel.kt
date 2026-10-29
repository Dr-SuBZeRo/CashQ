package com.cashq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashq.BuildConfig
import com.cashq.core.updates.GitHubReleaseClient
import com.cashq.core.updates.UpdateCheckResult
import com.cashq.data.local.PreferencesManager
import com.cashq.data.local.ThemeMode
import com.cashq.data.local.UserPreferences
import com.cashq.domain.model.CoinCountingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesManager.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _updateCheckState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateCheckState: StateFlow<UpdateCheckResult> = _updateCheckState.asStateFlow()

    private val _showModeWarningDialog = MutableStateFlow(false)
    val showModeWarningDialog: StateFlow<Boolean> = _showModeWarningDialog.asStateFlow()

    val installedVersionName: String = BuildConfig.VERSION_NAME

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicColor(enabled)
        }
    }

    fun setCoinsVisible(visible: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCoinsVisible(visible)
        }
    }

    fun requestCoinCountingMode(newMode: CoinCountingMode) {
        val currentMode = preferences.value.coinCountingMode
        if (currentMode == newMode) return

        if (newMode == CoinCountingMode.SEPARATE && currentMode == CoinCountingMode.COMBINED) {
            // Warn user about combined -> separate loss of note/coin split
            _showModeWarningDialog.value = true
        } else {
            setCoinCountingMode(newMode)
        }
    }

    fun confirmModeChange() {
        _showModeWarningDialog.value = false
        setCoinCountingMode(CoinCountingMode.SEPARATE)
    }

    fun dismissModeWarningDialog() {
        _showModeWarningDialog.value = false
    }

    private fun setCoinCountingMode(mode: CoinCountingMode) {
        viewModelScope.launch {
            preferencesManager.setCoinCountingMode(mode)
        }
    }

    fun checkForUpdates() {
        _updateCheckState.value = UpdateCheckResult.Checking
        viewModelScope.launch {
            val result = GitHubReleaseClient.checkForUpdates(installedVersionName)
            _updateCheckState.value = result
        }
    }
}

class SettingsViewModelFactory(
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(preferencesManager) as T
    }
}
