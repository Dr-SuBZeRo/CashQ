package com.cashq.ui.counter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
class CleanNumberInputFieldTest {

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
        override suspend fun setThemeMode(mode: ThemeMode) { stateFlow.value = stateFlow.value.copy(themeMode = mode) }
        override suspend fun setDynamicColor(enabled: Boolean) { stateFlow.value = stateFlow.value.copy(dynamicColor = enabled) }
        override suspend fun setCoinsVisible(visible: Boolean) { stateFlow.value = stateFlow.value.copy(coinsVisible = visible) }
        override suspend fun setCoinCountingMode(mode: CoinCountingMode) { stateFlow.value = stateFlow.value.copy(coinCountingMode = mode) }
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
    fun typing25_intoFieldInitiallyZero_produces25_not025() {
        // Step 1: Field has "0"
        val initial = TextFieldValue(text = "0", selection = TextRange(1))

        // Step 2: User types '2' at the end -> raw input is "02"
        val afterTyping2 = TextFieldValue(text = "02", selection = TextRange(2))
        val normalized1 = CleanNumberInputHelper.filterAndNormalize(initial, afterTyping2)
        assertEquals("2", normalized1.text)
        assertEquals(1, normalized1.selection.end)

        // Step 3: User types '5' at the end -> raw input is "25"
        val afterTyping5 = TextFieldValue(text = "25", selection = TextRange(2))
        val normalized2 = CleanNumberInputHelper.filterAndNormalize(normalized1, afterTyping5)
        assertEquals("25", normalized2.text)
        assertEquals(2, normalized2.selection.end)
    }

    @Test
    fun typing1000_produces1000() {
        var current = TextFieldValue(text = "", selection = TextRange(0))

        // Type '1'
        current = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("1", TextRange(1)))
        assertEquals("1", current.text)

        // Type '0'
        current = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("10", TextRange(2)))
        assertEquals("10", current.text)

        // Type '0'
        current = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("100", TextRange(3)))
        assertEquals("100", current.text)

        // Type '0'
        current = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("1000", TextRange(4)))
        assertEquals("1000", current.text)
        assertEquals(4, current.selection.end)
    }

    @Test
    fun clearingField_leavesItVisuallyEmpty_calculatesAsZero() {
        val initial = TextFieldValue(text = "25", selection = TextRange(2))

        // Backspace '5' -> "2"
        val afterOneBackspace = CleanNumberInputHelper.filterAndNormalize(initial, TextFieldValue("2", TextRange(1)))
        assertEquals("2", afterOneBackspace.text)
        assertEquals(2, CleanNumberInputHelper.parseQuantity(afterOneBackspace.text))

        // Backspace '2' -> ""
        val afterSecondBackspace = CleanNumberInputHelper.filterAndNormalize(afterOneBackspace, TextFieldValue("", TextRange(0)))
        assertEquals("", afterSecondBackspace.text)
        // Numeric interpretation must be zero:
        assertEquals(0, CleanNumberInputHelper.parseQuantity(afterSecondBackspace.text))
    }

    @Test
    fun enteringDigitAfterClearing_worksCorrectly() {
        val empty = TextFieldValue(text = "", selection = TextRange(0))

        // User types '7' after clearing
        val result = CleanNumberInputHelper.filterAndNormalize(empty, TextFieldValue("7", TextRange(1)))
        assertEquals("7", result.text)
        assertEquals(1, result.selection.end)
        assertEquals(7, CleanNumberInputHelper.parseQuantity(result.text))
    }

    @Test
    fun editingExistingNonzeroQuantity_worksCorrectly() {
        // User has "100" and places cursor between '1' and '0' (selection = 1), typing '5' -> "1500"
        val current = TextFieldValue(text = "100", selection = TextRange(1))
        val edited = TextFieldValue(text = "1500", selection = TextRange(2))
        val result = CleanNumberInputHelper.filterAndNormalize(current, edited)

        assertEquals("1500", result.text)
        assertEquals(2, result.selection.end)
        assertEquals(1500, CleanNumberInputHelper.parseQuantity(result.text))
    }

    @Test
    fun invalidCharactersAndNegativeValues_areRejected() {
        val current = TextFieldValue(text = "25", selection = TextRange(2))

        // Try negative sign "-25"
        val negative = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("-25", TextRange(3)))
        assertEquals("25", negative.text)

        // Try letters "25abc"
        val letters = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("25abc", TextRange(5)))
        assertEquals("25", letters.text)

        // Try decimal "25.5"
        val decimal = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("25.5", TextRange(4)))
        assertEquals("255", decimal.text)

        // Try special characters
        val special = CleanNumberInputHelper.filterAndNormalize(current, TextFieldValue("25$#", TextRange(4)))
        assertEquals("25", special.text)
    }

    @Test
    fun pastedLeadingZeros_areNormalized() {
        val empty = TextFieldValue(text = "", selection = TextRange(0))

        // User pastes "007" -> normalizes to "7"
        val result = CleanNumberInputHelper.filterAndNormalize(empty, TextFieldValue("007", TextRange(3)))
        assertEquals("7", result.text)
        assertEquals(1, result.selection.end)

        // User types "0" into empty -> remains "0"
        val zeroResult = CleanNumberInputHelper.filterAndNormalize(empty, TextFieldValue("0", TextRange(1)))
        assertEquals("0", zeroResult.text)
    }

    @Test
    fun subtotalsAndTotalCash_updateCorrectlyInViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeSessionRepository()
        val fakePrefs = FakePreferencesManager()
        val viewModel = CounterViewModel(fakeRepo, fakePrefs)
        advanceUntilIdle()

        // 1. Initially total is 0
        assertEquals(0L, viewModel.uiState.value.calculationResult.activeTotalRupees)

        // 2. Type "25" for ₹500 note (25 * 500 = 12,500)
        viewModel.onQuantityChanged(Denomination.NOTE_500.id, "25")
        advanceUntilIdle()

        assertEquals(12500L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(25L, viewModel.uiState.value.calculationResult.activeTotalUnits)
        assertEquals(1250000L, viewModel.uiState.value.calculationResult.denominationSubtotals[Denomination.NOTE_500.id])

        // 3. Clear the field (empty string)
        viewModel.onQuantityChanged(Denomination.NOTE_500.id, "")
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(0L, viewModel.uiState.value.calculationResult.activeTotalUnits)
        assertFalse(viewModel.uiState.value.quantities.containsKey(Denomination.NOTE_500.id))
    }

    @Test
    fun switchingModesOrCoinVisibility_doesNotLoseQuantities() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.SEPARATE)
        )
        val viewModel = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        // Enter notes and coins
        viewModel.onQuantityChanged(Denomination.NOTE_500.id, "10") // ₹5,000
        viewModel.onQuantityChanged(Denomination.COIN_5.id, "20")   // ₹100
        advanceUntilIdle()

        assertEquals(5100L, viewModel.uiState.value.calculationResult.activeTotalRupees)

        // Toggle coins off
        fakePrefs.setCoinsVisible(false)
        advanceUntilIdle()

        // Total reflects notes only, but coin quantity is preserved
        assertEquals(5000L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(20, viewModel.uiState.value.quantities[Denomination.COIN_5.id])

        // Toggle coins back on
        fakePrefs.setCoinsVisible(true)
        advanceUntilIdle()

        // Full total restored
        assertEquals(5100L, viewModel.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(20, viewModel.uiState.value.quantities[Denomination.COIN_5.id])
    }
}

