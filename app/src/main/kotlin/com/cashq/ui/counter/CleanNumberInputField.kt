package com.cashq.ui.counter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pure logic helper for normalizing and filtering quantity numeric input.
 * Ensures zero-placeholder replacement, leading-zero normalization,
 * character validation, and cursor preservation.
 */
object CleanNumberInputHelper {

    const val DEFAULT_MAX_DIGITS = 7

    /**
     * Normalizes and sanitizes incoming [TextFieldValue] changes:
     * - Rejects non-digit characters and negative numbers.
     * - If field was initially "0" and user types a nonzero digit, replaces "0" instead of producing "025".
     * - Strips unneeded leading zeros while editing (e.g. "025" -> "25", "007" -> "7").
     * - Keeps empty string "" when user deletes all characters (does not force 0).
     * - Caps max digit length to prevent integer overflow.
     * - Preserves valid cursor placement.
     */
    fun filterAndNormalize(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
        maxDigits: Int = DEFAULT_MAX_DIGITS
    ): TextFieldValue {
        val raw = newValue.text

        // Empty field allowed while editing
        if (raw.isEmpty()) {
            return TextFieldValue(text = "", selection = TextRange(0))
        }

        // Filter out non-digit characters (rejects negatives, spaces, decimals, letters)
        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) {
            return TextFieldValue(text = "", selection = TextRange(0))
        }

        // Case: Initial field was "0" and user typed a digit
        if (oldValue.text == "0" && digitsOnly.length > 1) {
            // If user typed at the end (e.g. "02"), replace "0" with "2"
            if (digitsOnly.startsWith("0")) {
                val replaced = digitsOnly.dropWhile { it == '0' }
                val resultText = if (replaced.isEmpty()) "0" else replaced
                val target = if (resultText.length > maxDigits) resultText.take(maxDigits) else resultText
                return TextFieldValue(text = target, selection = TextRange(target.length))
            } else {
                // If user typed before "0" (e.g. "20"), keep "20"
                val normalized = digitsOnly.dropWhile { it == '0' }
                val resultText = if (normalized.isEmpty()) "0" else normalized
                val target = if (resultText.length > maxDigits) resultText.take(maxDigits) else resultText
                return TextFieldValue(text = target, selection = TextRange(target.length))
            }
        }

        // Case: General leading zero removal for multi-digit numbers (e.g. pasted "0025" or typed "025")
        val normalizedDigits = if (digitsOnly.length > 1 && digitsOnly.startsWith("0")) {
            val stripped = digitsOnly.dropWhile { it == '0' }
            if (stripped.isEmpty()) "0" else stripped
        } else {
            digitsOnly
        }

        // Enforce max length constraint
        val finalDigits = if (normalizedDigits.length > maxDigits) {
            // If new input exceeds max digits, keep previous value or truncate
            if (oldValue.text.length >= maxDigits) {
                return oldValue
            } else {
                normalizedDigits.take(maxDigits)
            }
        } else {
            normalizedDigits
        }

        // Compute safe cursor position
        val droppedLeadingZeros = digitsOnly.length - normalizedDigits.length
        val rawCursor = newValue.selection.end - droppedLeadingZeros
        val safeCursor = rawCursor.coerceIn(0, finalDigits.length)

        return TextFieldValue(
            text = finalDigits,
            selection = TextRange(safeCursor)
        )
    }

    /**
     * Parses numeric quantity from string, treating empty or blank as 0.
     */
    fun parseQuantity(text: String): Int {
        val digits = text.filter { it.isDigit() }
        return if (digits.isEmpty()) 0 else digits.toIntOrNull() ?: 0
    }
}

/**
 * Robust quantity input text field for CashQ denomination rows.
 * Separates editable text representation from numeric calculation value,
 * prevents "025" zero-prefixing, avoids cursor jumping, and allows temporary empty states.
 */
@Composable
fun CleanNumberInputField(
    quantity: Int,
    onQuantityChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    enabled: Boolean = true,
    maxDigits: Int = CleanNumberInputHelper.DEFAULT_MAX_DIGITS
) {
    // Local editable text state initialized from quantity
    var textFieldValue by remember {
        val initialText = if (quantity > 0) quantity.toString() else ""
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
    }

    // Synchronize external quantity changes (e.g. Reset, Session Load, Mode Conversions)
    // without interrupting active typing when the numeric value matches.
    val currentNumeric = CleanNumberInputHelper.parseQuantity(textFieldValue.text)
    LaunchedEffect(quantity) {
        if (quantity != currentNumeric) {
            val newText = if (quantity > 0) quantity.toString() else ""
            textFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(newText.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val normalized = CleanNumberInputHelper.filterAndNormalize(
                oldValue = textFieldValue,
                newValue = newValue,
                maxDigits = maxDigits
            )
            textFieldValue = normalized
            onQuantityChanged(normalized.text)
        },
        placeholder = {
            Text(
                text = "0",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { onDone() }),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    )
}

