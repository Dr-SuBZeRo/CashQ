package com.cashq.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cashq.domain.model.CoinCountingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cashq_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val coinsVisible: Boolean = true,
    val coinCountingMode: CoinCountingMode = CoinCountingMode.SEPARATE
)

interface PreferencesManager {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setCoinsVisible(visible: Boolean)
    suspend fun setCoinCountingMode(mode: CoinCountingMode)
}

fun PreferencesManager(context: Context): PreferencesManager = PreferencesManagerImpl(context)

internal class PreferencesManagerImpl(private val context: Context) : PreferencesManager {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COINS_VISIBLE = booleanPreferencesKey("coins_visible")
        val COIN_COUNTING_MODE = stringPreferencesKey("coin_counting_mode")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeStr)
            } catch (_: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }

            val dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true
            val coinsVisible = preferences[PreferencesKeys.COINS_VISIBLE] ?: true
            val coinModeStr = preferences[PreferencesKeys.COIN_COUNTING_MODE] ?: CoinCountingMode.SEPARATE.name
            val coinMode = try {
                CoinCountingMode.valueOf(coinModeStr)
            } catch (_: IllegalArgumentException) {
                CoinCountingMode.SEPARATE
            }

            UserPreferences(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                coinsVisible = coinsVisible,
                coinCountingMode = coinMode
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setCoinsVisible(visible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COINS_VISIBLE] = visible
        }
    }

    override suspend fun setCoinCountingMode(mode: CoinCountingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COIN_COUNTING_MODE] = mode.name
        }
    }
}
