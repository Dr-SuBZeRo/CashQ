package com.cashq

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cashq.ui.counter.CounterScreenContent
import com.cashq.ui.counter.CounterUiState
import com.cashq.ui.theme.CashQTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI test for the CashQ Counter screen content.
 */
class CounterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun counterScreen_displaysTotalZeroInitially() {
        composeTestRule.setContent {
            CashQTheme {
                CounterScreenContent(uiState = CounterUiState())
            }
        }
        composeTestRule.onNodeWithText("₹0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Cash").assertIsDisplayed()
    }

    @Test
    fun counterScreen_displaysAppTitle() {
        composeTestRule.setContent {
            CashQTheme {
                CounterScreenContent(uiState = CounterUiState())
            }
        }
        composeTestRule.onNodeWithText("CashQ").assertIsDisplayed()
    }
}
