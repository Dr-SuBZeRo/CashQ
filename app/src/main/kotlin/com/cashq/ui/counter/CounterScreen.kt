package com.cashq.ui.counter

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashq.R
import com.cashq.core.formatting.CurrencyFormatter
import com.cashq.domain.model.CurrencyType
import com.cashq.domain.model.Denomination
import com.cashq.ui.navigation.CashQTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    viewModel: CounterViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showShareFormatDialog by remember { mutableStateOf(false) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }
    var activeShareData by remember { mutableStateOf<com.cashq.core.sharing.CashCountShareData?>(null) }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearNotification()
            }
        }
    }

    CounterScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenDrawer = onOpenDrawer,
        onQuantityChanged = { id, raw -> viewModel.onQuantityChanged(id, raw) },
        onToggleCoins = { viewModel.onToggleCoinsVisible(it) },
        onSaveClick = { viewModel.openSaveDialog() },
        onShareClick = {
            activeShareData = viewModel.createShareData()
            showShareFormatDialog = true
        },
        onResetClick = { viewModel.openResetDialog() },
        onDismissSave = { viewModel.dismissSaveDialog() },
        onConfirmSave = { title -> viewModel.saveCurrentCount(title) },
        onDismissReset = { viewModel.dismissResetDialog() },
        onConfirmReset = { viewModel.confirmReset() },
        onDismissModeChange = { viewModel.dismissModeChangeDialog() },
        onConfirmModeChange = { viewModel.confirmModeChange() },
        modifier = modifier
    )

    activeShareData?.let { data ->
        if (showShareFormatDialog) {
            com.cashq.ui.sharing.ShareFormatSelectionDialog(
                shareData = data,
                onDismiss = { showShareFormatDialog = false },
                onSelectFormat = { format ->
                    showShareFormatDialog = false
                    when (format) {
                        com.cashq.ui.sharing.ShareFormat.RICH_TEXT -> {
                            com.cashq.core.sharing.ShareManager.shareRichText(context, data)
                        }
                        com.cashq.ui.sharing.ShareFormat.EMAIL -> {
                            com.cashq.core.sharing.ShareManager.shareEmail(context, data)
                        }
                        com.cashq.ui.sharing.ShareFormat.IMAGE -> {
                            showImagePreviewDialog = true
                        }
                    }
                }
            )
        }

        if (showImagePreviewDialog) {
            com.cashq.ui.sharing.SummaryCardPreviewDialog(
                shareData = data,
                onDismiss = { showImagePreviewDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreenContent(
    uiState: CounterUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenDrawer: () -> Unit = {},
    onQuantityChanged: (String, String) -> Unit = { _, _ -> },
    onToggleCoins: (Boolean) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onResetClick: () -> Unit = {},
    onDismissSave: () -> Unit = {},
    onConfirmSave: (String) -> Unit = {},
    onDismissReset: () -> Unit = {},
    onConfirmReset: () -> Unit = {},
    onDismissModeChange: () -> Unit = {},
    onConfirmModeChange: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CashQTopAppBar(
                title = "CashQ",
                subtitle = "Indian Rupee Cash Counter",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Prominent Total Card
                TotalCard(
                    totalRupees = uiState.calculationResult.activeTotalRupees,
                    totalUnits = uiState.calculationResult.activeTotalUnits,
                    noteRupees = uiState.calculationResult.noteTotalPaise / 100L,
                    noteUnits = uiState.calculationResult.noteUnits,
                    coinRupees = uiState.calculationResult.coinTotalPaise / 100L,
                    coinUnits = uiState.calculationResult.coinUnits,
                    coinsVisible = uiState.coinsVisible
                )
            }

            item {
                // Action Buttons Row
                ActionButtonsRow(
                    onSaveClick = onSaveClick,
                    onShareClick = onShareClick,
                    onResetClick = onResetClick
                )
            }

            item {
                // Show coins toggle switch card
                CoinsVisibilityToggleCard(
                    coinsVisible = uiState.coinsVisible,
                    onToggle = onToggleCoins
                )
            }

            // Denomination Rows
            items(uiState.visibleDenominations, key = { it.id }) { denom ->
                val quantity = uiState.quantities[denom.id] ?: 0
                val subtotalPaise = uiState.calculationResult.denominationSubtotals[denom.id] ?: 0L

                DenominationRow(
                    denomination = denom,
                    quantity = quantity,
                    subtotalRupees = subtotalPaise / 100L,
                    onQuantityChanged = { newRaw ->
                        onQuantityChanged(denom.id, newRaw)
                    },
                    onDone = { focusManager.clearFocus() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Save Dialog
    if (uiState.showSaveDialog) {
        SaveCountDialog(
            totalRupees = uiState.calculationResult.activeTotalRupees,
            onDismiss = onDismissSave,
            onConfirm = onConfirmSave
        )
    }

    // Reset Confirmation Dialog
    if (uiState.showResetDialog) {
        ResetConfirmDialog(
            onDismiss = onDismissReset,
            onConfirm = onConfirmReset
        )
    }

    // Mode Change Notice Dialog
    if (uiState.showModeChangeNoticeDialog) {
        ModeChangeNoticeDialog(
            onDismiss = onDismissModeChange,
            onConfirm = onConfirmModeChange
        )
    }
}

@Composable
fun TotalCard(
    totalRupees: Long,
    totalUnits: Long,
    noteRupees: Long,
    noteUnits: Long,
    coinRupees: Long,
    coinUnits: Long,
    coinsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Cash",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = CurrencyFormatter.formatRupees(totalRupees),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${CurrencyFormatter.formatUnits(totalUnits)} units",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                if (!coinsVisible) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Notes only",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${CurrencyFormatter.formatRupees(noteRupees)} (${CurrencyFormatter.formatUnits(noteUnits)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (coinsVisible) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${CurrencyFormatter.formatRupees(coinRupees)} (${CurrencyFormatter.formatUnits(coinUnits)})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save")
        }

        OutlinedButton(
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share")
        }

        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset")
        }
    }
}

@Composable
fun CoinsVisibilityToggleCard(
    coinsVisible: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show coin denominations",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (coinsVisible) "Coins included in active total" else "Hidden coins preserved; total reflects notes only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = coinsVisible,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun DenominationRow(
    denomination: Denomination,
    quantity: Int,
    subtotalRupees: Long,
    onQuantityChanged: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawText by remember(quantity) {
        mutableStateOf(if (quantity > 0) quantity.toString() else "")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quantity > 0)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (quantity > 0) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Denomination Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    denomination.isCombinedEntry -> MaterialTheme.colorScheme.tertiaryContainer
                    denomination.type == CurrencyType.NOTE -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.width(96.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = denomination.shortLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            denomination.isCombinedEntry -> MaterialTheme.colorScheme.onTertiaryContainer
                            denomination.type == CurrencyType.NOTE -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = when {
                            denomination.isCombinedEntry -> "Combined"
                            denomination.type == CurrencyType.NOTE -> "Note"
                            else -> "Coin"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Quantity Input Field
            OutlinedTextField(
                value = rawText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    rawText = filtered
                    onQuantityChanged(filtered)
                },
                placeholder = { Text("0", textAlign = TextAlign.Center) },
                singleLine = true,
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
                modifier = Modifier
                    .width(100.dp)
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Subtotal
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Subtotal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatRupees(subtotalRupees),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (subtotalRupees > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun SaveCountDialog(
    totalRupees: Long,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.BookmarkBorder, contentDescription = null) },
        title = { Text("Save Cash Count") },
        text = {
            Column {
                Text(
                    text = "Total: ${CurrencyFormatter.formatRupees(totalRupees)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title (Optional)") },
                    placeholder = { Text("e.g. Clinic Closing Cash") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Leave blank to use current date & time automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResetConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
        title = { Text("Reset Count?") },
        text = {
            Text("This will clear all entered quantities back to zero. Your saved history will NOT be affected.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ModeChangeNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        title = { Text("Switch to Separate Mode") },
        text = {
            Text(
                "When converting from combined to separate counting, your combined ₹20 and ₹10 quantities will be assigned to note rows, and coin rows will be set to zero. The total cash value is completely preserved, but the original split between notes and coins cannot be reconstructed automatically."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
