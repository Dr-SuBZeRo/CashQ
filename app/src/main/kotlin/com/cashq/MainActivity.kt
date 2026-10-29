package com.cashq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashq.data.local.ThemeMode
import com.cashq.data.local.UserPreferences
import com.cashq.ui.navigation.CashQApp
import com.cashq.ui.theme.CashQTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val preferencesManager = (application as CashQApplication).preferencesManager
            val preferences by preferencesManager.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )

            val isDark = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CashQTheme(
                darkTheme = isDark,
                dynamicColor = preferences.dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CashQApp()
                }
            }
        }
    }
}
