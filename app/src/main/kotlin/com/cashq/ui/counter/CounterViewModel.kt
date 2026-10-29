package com.cashq.ui.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashq.core.sharing.ShareSummaryGenerator
import com.cashq.data.local.PreferencesManager
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.data.repository.SessionRepository
import com.cashq.domain.calculation.CashCalculationEngine
import com.cashq.domain.model.CalculationResult
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CounterUiState(
    val quantities: Map<String, Int> = emptyMap(),
    val coinsVisible: Boolean = true,
    val coinMode: CoinCountingMode = CoinCountingMode.SEPARATE,
    val calculationResult: CalculationResult = CalculationResult.ZERO,
    val visibleDenominations: List<Denomination> = Denomination.ALL_CANONICAL,
    val showResetDialog: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showModeChangeNoticeDialog: Boolean = false,
    val pendingModeChange: CoinCountingMode? = null,
    val userNotification: String? = null
)

class CounterViewModel(
    private val sessionRepository: SessionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    private var lastObservedMode: CoinCountingMode? = null
    private var isInitialized = false

    init {
        viewModelScope.launch {
            preferencesManager.userPreferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    val newVisible = prefs.coinsVisible
                    val newMode = prefs.coinCountingMode

                    val (updatedQuantities, updatedMode) = if (!isInitialized) {
                        isInitialized = true
                        lastObservedMode = newMode
                        current.quantities to newMode
                    } else if (lastObservedMode != null && lastObservedMode != newMode) {
                        // Denomination mode actually changed: perform atomic conversion
                        val converted = if (newMode == CoinCountingMode.COMBINED) {
                            CashCalculationEngine.convertSeparateToCombined(current.quantities)
                        } else {
                            CashCalculationEngine.convertCombinedToSeparate(current.quantities)
                        }
                        lastObservedMode = newMode
                        converted to newMode
                    } else {
                        current.quantities to (lastObservedMode ?: newMode)
                    }

                    val denoms = CashCalculationEngine.getVisibleDenominations(newVisible, updatedMode)
                    val calc = CashCalculationEngine.calculate(updatedQuantities, newVisible, updatedMode)

                    current.copy(
                        quantities = updatedQuantities,
                        coinsVisible = newVisible,
                        coinMode = updatedMode,
                        visibleDenominations = denoms,
                        calculationResult = calc
                    )
                }
            }
        }
    }

    /**
     * Updates the quantity for a specific denomination row.
     * Sanitizes input to whole non-negative numbers.
     */
    fun onQuantityChanged(denominationId: String, rawValue: String) {
        val sanitizedDigits = rawValue.filter { it.isDigit() }
        val qty = if (sanitizedDigits.isEmpty()) 0 else sanitizedDigits.toIntOrNull() ?: 0

        _uiState.update { current ->
            val updatedQuantities = current.quantities.toMutableMap()
            if (qty > 0) {
                updatedQuantities[denominationId] = qty
            } else {
                updatedQuantities.remove(denominationId)
            }
            val calc = CashCalculationEngine.calculate(
                updatedQuantities,
                current.coinsVisible,
                current.coinMode
            )
            current.copy(
                quantities = updatedQuantities,
                calculationResult = calc
            )
        }
    }

    /**
     * Toggles coin visibility.
     * Persists the preference in DataStore (single source of truth).
     * Hidden coin quantities are preserved in session state and restored when re-enabled.
     */
    fun onToggleCoinsVisible(visible: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCoinsVisible(visible)
        }
    }

    /**
     * Requests a coin counting mode change (Separate vs Combined).
     */
    fun onSelectCoinMode(newMode: CoinCountingMode) {
        val current = _uiState.value
        if (current.coinMode == newMode) return

        if (newMode == CoinCountingMode.SEPARATE &&
            ((current.quantities[Denomination.COMBINED_20.id] ?: 0) > 0 ||
                    (current.quantities[Denomination.COMBINED_10.id] ?: 0) > 0)
        ) {
            _uiState.update { it.copy(showModeChangeNoticeDialog = true, pendingModeChange = newMode) }
        } else {
            executeModeChange(newMode)
        }
    }

    fun confirmModeChange() {
        val pending = _uiState.value.pendingModeChange ?: return
        _uiState.update { it.copy(showModeChangeNoticeDialog = false, pendingModeChange = null) }
        executeModeChange(pending)
    }

    fun dismissModeChangeDialog() {
        _uiState.update { it.copy(showModeChangeNoticeDialog = false, pendingModeChange = null) }
    }

    private fun executeModeChange(newMode: CoinCountingMode) {
        viewModelScope.launch {
            preferencesManager.setCoinCountingMode(newMode)
        }
    }

    fun openResetDialog() {
        _uiState.update { it.copy(showResetDialog = true) }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetDialog = false) }
    }

    fun confirmReset() {
        _uiState.update { current ->
            val emptyMap = emptyMap<String, Int>()
            val calc = CashCalculationEngine.calculate(emptyMap, current.coinsVisible, current.coinMode)
            current.copy(
                quantities = emptyMap,
                calculationResult = calc,
                showResetDialog = false,
                userNotification = "Count reset to ₹0"
            )
        }
    }

    fun openSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = true) }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(showSaveDialog = false) }
    }

    fun saveCurrentCount(userTitle: String?) {
        val current = _uiState.value
        val now = System.currentTimeMillis()
        val defaultTitle = "Cash Count — " + SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("en-IN")).format(Date(now))
        val finalTitle = if (userTitle.isNullOrBlank()) defaultTitle else userTitle.trim()

        val sessionEntity = SavedSessionEntity(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            createdAt = now,
            updatedAt = now,
            totalPaise = current.calculationResult.activeTotalPaise,
            totalUnits = current.calculationResult.activeTotalUnits,
            coinsVisible = current.coinsVisible,
            coinMode = current.coinMode.name,
            quantitiesJson = SavedSessionEntity.serializeQuantities(current.quantities),
            isNotesOnly = current.calculationResult.isNotesOnly
        )

        viewModelScope.launch {
            sessionRepository.saveSession(sessionEntity)
            _uiState.update {
                it.copy(
                    showSaveDialog = false,
                    userNotification = "Session saved to History: $finalTitle"
                )
            }
        }
    }

    fun loadSession(session: SavedSessionEntity) {
        val loadedQuantities = session.getQuantitiesMap()
        val loadedMode = try {
            CoinCountingMode.valueOf(session.coinMode)
        } catch (_: Exception) {
            CoinCountingMode.SEPARATE
        }
        val loadedVisibility = session.coinsVisible

        lastObservedMode = loadedMode

        viewModelScope.launch {
            preferencesManager.setCoinsVisible(loadedVisibility)
            preferencesManager.setCoinCountingMode(loadedMode)
        }

        _uiState.update { current ->
            val denoms = CashCalculationEngine.getVisibleDenominations(loadedVisibility, loadedMode)
            val calc = CashCalculationEngine.calculate(loadedQuantities, loadedVisibility, loadedMode)
            current.copy(
                quantities = loadedQuantities,
                coinsVisible = loadedVisibility,
                coinMode = loadedMode,
                visibleDenominations = denoms,
                calculationResult = calc,
                userNotification = "Loaded session: ${session.title}"
            )
        }
    }

    fun generateShareSummary(): String {
        val current = _uiState.value
        val defaultTitle = "Cash Count — " + SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("en-IN")).format(Date())
        return ShareSummaryGenerator.generateSummary(
            title = defaultTitle,
            timestamp = System.currentTimeMillis(),
            quantities = current.quantities,
            coinsVisible = current.coinsVisible,
            coinMode = current.coinMode
        )
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}

class CounterViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CounterViewModel(sessionRepository, preferencesManager) as T
    }
}
