package com.cashq.ui.counter

import com.cashq.data.local.PreferencesManager
import com.cashq.data.local.ThemeMode
import com.cashq.data.local.UserPreferences
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.data.repository.SessionRepository
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterPreferenceSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSessionRepository : SessionRepository {
        override fun getAllSessions(): Flow<List<SavedSessionEntity>> = flowOf(emptyList())
        override suspend fun getSessionById(id: String): SavedSessionEntity? = null
        override suspend fun saveSession(session: SavedSessionEntity) {}
        override suspend fun deleteSession(id: String) {}
    }

    private class FakePreferencesManager(
        initial: UserPreferences = UserPreferences()
    ) : PreferencesManager {
        val stateFlow = MutableStateFlow(initial)

        override val userPreferencesFlow: Flow<UserPreferences> = stateFlow

        override suspend fun setThemeMode(mode: ThemeMode) {
            stateFlow.value = stateFlow.value.copy(themeMode = mode)
        }

        override suspend fun setDynamicColor(enabled: Boolean) {
            stateFlow.value = stateFlow.value.copy(dynamicColor = enabled)
        }

        override suspend fun setCoinsVisible(visible: Boolean) {
            stateFlow.value = stateFlow.value.copy(coinsVisible = visible)
        }

        override suspend fun setCoinCountingMode(mode: CoinCountingMode) {
            stateFlow.value = stateFlow.value.copy(coinCountingMode = mode)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCoinVisibilitySync_hidesCoinsAndPreservesEnteredQuantities() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.SEPARATE)
        )
        val viewModel = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        // Enter quantities: 2x ₹500 note (₹1,000) and 10x ₹5 coin (₹50)
        viewModel.onQuantityChanged(Denomination.NOTE_500.id, "2")
        viewModel.onQuantityChanged(Denomination.COIN_5.id, "10")
        advanceUntilIdle()

        assertEquals(1050L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(2, viewModel.uiState.value.quantities[Denomination.NOTE_500.id])
        assertEquals(10, viewModel.uiState.value.quantities[Denomination.COIN_5.id])

        // External/Settings change: coin visibility toggled OFF
        fakePrefs.setCoinsVisible(false)
        advanceUntilIdle()

        // Coins should now be hidden, excluded from calculation, but preserved in state
        assertFalse(viewModel.uiState.value.coinsVisible)
        assertEquals(1000L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertFalse(viewModel.uiState.value.visibleDenominations.any { it.id == Denomination.COIN_5.id })
        assertEquals(10, viewModel.uiState.value.quantities[Denomination.COIN_5.id])

        // External/Settings change: coin visibility toggled ON
        fakePrefs.setCoinsVisible(true)
        advanceUntilIdle()

        // Coins restored, calculation returns to full ₹1,050
        assertTrue(viewModel.uiState.value.coinsVisible)
        assertEquals(1050L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertTrue(viewModel.uiState.value.visibleDenominations.any { it.id == Denomination.COIN_5.id })
        assertEquals(10, viewModel.uiState.value.quantities[Denomination.COIN_5.id])
    }

    @Test
    fun testModeSwitchSeparateToCombined_mergesDenominationsAndPreservesValue() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.SEPARATE)
        )
        val viewModel = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        // Enter: 10x ₹20 note (₹200) + 5x ₹20 coin (₹100) = ₹300
        // Enter: 4x ₹10 note (₹40) + 6x ₹10 coin (₹60) = ₹100
        // Total = ₹400
        viewModel.onQuantityChanged(Denomination.NOTE_20.id, "10")
        viewModel.onQuantityChanged(Denomination.COIN_20.id, "5")
        viewModel.onQuantityChanged(Denomination.NOTE_10.id, "4")
        viewModel.onQuantityChanged(Denomination.COIN_10.id, "6")
        advanceUntilIdle()

        assertEquals(400L, viewModel.uiState.value.calculationResult.activeTotalRupees)

        // Settings change: switch to COMBINED mode
        fakePrefs.setCoinCountingMode(CoinCountingMode.COMBINED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CoinCountingMode.COMBINED, state.coinMode)
        assertEquals(15, state.quantities[Denomination.COMBINED_20.id])
        assertEquals(10, state.quantities[Denomination.COMBINED_10.id])
        assertFalse(state.quantities.containsKey(Denomination.NOTE_20.id))
        assertFalse(state.quantities.containsKey(Denomination.COIN_20.id))
        assertEquals(400L, state.calculationResult.activeTotalRupees)
    }

    @Test
    fun testModeSwitchCombinedToSeparate_assignsToNotesZeroesCoinsAndPreservesValue() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.COMBINED)
        )
        val viewModel = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        // Enter: 15x ₹20 combined (₹300) and 10x ₹10 combined (₹100) = ₹400
        viewModel.onQuantityChanged(Denomination.COMBINED_20.id, "15")
        viewModel.onQuantityChanged(Denomination.COMBINED_10.id, "10")
        advanceUntilIdle()

        assertEquals(400L, viewModel.uiState.value.calculationResult.activeTotalRupees)

        // Settings change: switch to SEPARATE mode
        fakePrefs.setCoinCountingMode(CoinCountingMode.SEPARATE)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CoinCountingMode.SEPARATE, state.coinMode)
        assertEquals(15, state.quantities[Denomination.NOTE_20.id])
        assertEquals(0, state.quantities[Denomination.COIN_20.id])
        assertEquals(10, state.quantities[Denomination.NOTE_10.id])
        assertEquals(0, state.quantities[Denomination.COIN_10.id])
        assertEquals(400L, state.calculationResult.activeTotalRupees)
    }

    @Test
    fun testOtherPreferenceChanges_doNotReTriggerConversions() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.SEPARATE)
        )
        val viewModel = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        viewModel.onQuantityChanged(Denomination.NOTE_20.id, "7")
        viewModel.onQuantityChanged(Denomination.COIN_20.id, "3")
        advanceUntilIdle()

        // Change dynamicColor or theme in preferences
        fakePrefs.setDynamicColor(false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.quantities[Denomination.NOTE_20.id])
        assertEquals(3, state.quantities[Denomination.COIN_20.id])
        assertEquals(200L, state.calculationResult.activeTotalRupees)
    }
}
