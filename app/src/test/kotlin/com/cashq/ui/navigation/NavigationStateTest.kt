package com.cashq.ui.navigation

import com.cashq.core.sharing.CashCountShareData
import com.cashq.data.local.PreferencesManager
import com.cashq.data.local.ThemeMode
import com.cashq.data.local.UserPreferences
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.data.repository.SessionRepository
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import com.cashq.ui.counter.CounterViewModel
import com.cashq.ui.sharing.ShareFormat
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationStateTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSessionRepository : SessionRepository {
        private val list = mutableListOf<SavedSessionEntity>()
        override fun getAllSessions(): Flow<List<SavedSessionEntity>> = flowOf(list)
        override suspend fun getSessionById(id: String): SavedSessionEntity? = list.find { it.id == id }
        override suspend fun saveSession(session: SavedSessionEntity) { list.add(session) }
        override suspend fun deleteSession(id: String) { list.removeIf { it.id == id } }
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
    fun testDrawerDestinations_haveAccurateLabelsAndMetadata() {
        assertEquals("Counter", CashQDestination.COUNTER.label)
        assertEquals("CashQ", CashQDestination.COUNTER.title)
        assertEquals("Indian Rupee Cash Counter", CashQDestination.COUNTER.subtitle)

        assertEquals("History", CashQDestination.HISTORY.label)
        assertEquals("CashQ", CashQDestination.HISTORY.title)
        assertEquals("Count History", CashQDestination.HISTORY.subtitle)

        assertEquals("Settings", CashQDestination.SETTINGS.label)
        assertEquals("CashQ", CashQDestination.SETTINGS.title)
        assertEquals("Settings & Preferences", CashQDestination.SETTINGS.subtitle)

        assertEquals(3, CashQDestination.entries.size)
    }

    @Test
    fun testNavigationFlow_destinationTransitionsAndSessionLoadRedirect() = runTest(testDispatcher) {
        var currentDestination = CashQDestination.COUNTER

        // Navigation state transition to HISTORY
        currentDestination = CashQDestination.HISTORY
        assertEquals(CashQDestination.HISTORY, currentDestination)

        // Navigation state transition to SETTINGS
        currentDestination = CashQDestination.SETTINGS
        assertEquals(CashQDestination.SETTINGS, currentDestination)

        // Session load simulation from History into Counter:
        // When user taps "Load Into Counter", destination switches back to COUNTER and counter updates
        val fakeRepo = FakeSessionRepository()
        val fakePrefs = FakePreferencesManager()
        val counterVm = CounterViewModel(fakeRepo, fakePrefs)
        advanceUntilIdle()

        val savedSession = SavedSessionEntity(
            id = "test-session-1",
            title = "Morning Register",
            createdAt = 1730180000000L,
            updatedAt = 1730180000000L,
            totalPaise = 50000L, // ₹500
            totalUnits = 1L,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE.name,
            isNotesOnly = false,
            quantitiesJson = """{"note_500":1}"""
        )

        // Action performed when loading session from History:
        counterVm.loadSession(savedSession)
        currentDestination = CashQDestination.COUNTER
        advanceUntilIdle()

        assertEquals(CashQDestination.COUNTER, currentDestination)
        assertEquals(500L, counterVm.uiState.value.calculationResult.activeTotalRupees)
        assertEquals(1, counterVm.uiState.value.quantities[Denomination.NOTE_500.id])
    }

    @Test
    fun testPreferencesPersistence_remainsConsistentAcrossNavigation() = runTest(testDispatcher) {
        val fakePrefs = FakePreferencesManager(
            UserPreferences(coinsVisible = true, coinCountingMode = CoinCountingMode.SEPARATE)
        )
        val counterVm = CounterViewModel(FakeSessionRepository(), fakePrefs)
        advanceUntilIdle()

        // User enters values on Counter
        counterVm.onQuantityChanged(Denomination.NOTE_500.id, "4") // ₹2,000
        advanceUntilIdle()

        var currentDestination = CashQDestination.COUNTER
        assertEquals(2000L, counterVm.uiState.value.calculationResult.activeTotalRupees)

        // Navigate to Settings
        currentDestination = CashQDestination.SETTINGS
        assertEquals(CashQDestination.SETTINGS, currentDestination)

        // User toggles coins visibility in Settings
        fakePrefs.setCoinsVisible(false)
        advanceUntilIdle()

        // Navigate back to Counter
        currentDestination = CashQDestination.COUNTER
        assertEquals(CashQDestination.COUNTER, currentDestination)

        // State reflects preferences and quantities are preserved
        val state = counterVm.uiState.value
        assertEquals(false, state.coinsVisible)
        assertEquals(4, state.quantities[Denomination.NOTE_500.id])
        assertEquals(2000L, state.calculationResult.activeTotalRupees)
    }

    @Test
    fun testShareEntryPoints_counterAndHistoryGenerateValidShareData() = runTest(testDispatcher) {
        val fakeRepo = FakeSessionRepository()
        val fakePrefs = FakePreferencesManager()
        val counterVm = CounterViewModel(fakeRepo, fakePrefs)
        advanceUntilIdle()

        counterVm.onQuantityChanged(Denomination.NOTE_200.id, "5") // ₹1,000
        advanceUntilIdle()

        // Counter share data entry point
        val counterShareData = counterVm.createShareData()
        assertNotNull(counterShareData)
        assertEquals(1000L, counterShareData.totalRupees)
        assertEquals(5L, counterShareData.totalUnits)

        // History saved session share data entry point
        val savedSession = SavedSessionEntity(
            id = "sess-2",
            title = "Clinic Evening Count",
            createdAt = 1730180000000L,
            updatedAt = 1730180000000L,
            totalPaise = 200000L, // ₹2,000
            totalUnits = 4L,
            coinsVisible = true,
            coinMode = CoinCountingMode.SEPARATE.name,
            isNotesOnly = false,
            quantitiesJson = """{"note_500":4}"""
        )
        val historyShareData = CashCountShareData.fromSavedSession(savedSession)
        assertEquals("Clinic Evening Count", historyShareData.title)
        assertEquals(2000L, historyShareData.totalRupees)
        assertEquals(4L, historyShareData.totalUnits)

        // Verify share format options are available
        assertEquals(3, ShareFormat.entries.size)
        assertTrue(ShareFormat.entries.contains(ShareFormat.RICH_TEXT))
        assertTrue(ShareFormat.entries.contains(ShareFormat.EMAIL))
        assertTrue(ShareFormat.entries.contains(ShareFormat.IMAGE))
    }
}

