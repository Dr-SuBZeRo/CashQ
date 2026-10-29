package com.cashq.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cashq.core.sharing.ShareSummaryGenerator
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.data.repository.SessionRepository
import com.cashq.domain.model.CoinCountingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val searchQuery: String = "",
    val sessionToDelete: SavedSessionEntity? = null,
    val selectedSessionForDetails: SavedSessionEntity? = null,
    val userNotification: String? = null
)

class HistoryViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    val sessions: StateFlow<List<SavedSessionEntity>> = sessionRepository.getAllSessions()
        .combine(_uiState) { list, state ->
            if (state.searchQuery.isBlank()) {
                list
            } else {
                list.filter { it.title.contains(state.searchQuery, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun requestDelete(session: SavedSessionEntity) {
        _uiState.update { it.copy(sessionToDelete = session) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(sessionToDelete = null) }
    }

    fun confirmDelete() {
        val session = _uiState.value.sessionToDelete ?: return
        viewModelScope.launch {
            sessionRepository.deleteSession(session.id)
            _uiState.update {
                it.copy(
                    sessionToDelete = null,
                    userNotification = "Deleted session: ${session.title}"
                )
            }
        }
    }

    fun openDetails(session: SavedSessionEntity) {
        _uiState.update { it.copy(selectedSessionForDetails = session) }
    }

    fun closeDetails() {
        _uiState.update { it.copy(selectedSessionForDetails = null) }
    }

    fun generateShareSummary(session: SavedSessionEntity): String {
        val mode = try {
            CoinCountingMode.valueOf(session.coinMode)
        } catch (_: Exception) {
            CoinCountingMode.SEPARATE
        }
        return ShareSummaryGenerator.generateSummary(
            title = session.title,
            timestamp = session.createdAt,
            quantities = session.getQuantitiesMap(),
            coinsVisible = session.coinsVisible,
            coinMode = mode
        )
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}

class HistoryViewModelFactory(
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HistoryViewModel(sessionRepository) as T
    }
}
