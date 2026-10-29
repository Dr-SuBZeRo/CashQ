package com.cashq.ui.history

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cashq.core.formatting.CurrencyFormatter
import com.cashq.data.local.entity.SavedSessionEntity
import com.cashq.domain.calculation.CashCalculationEngine
import com.cashq.domain.model.CoinCountingMode
import com.cashq.domain.model.Denomination
import com.cashq.ui.navigation.CashQTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onLoadSessionIntoCounter: (SavedSessionEntity) -> Unit,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var shareDataToShare by remember { mutableStateOf<com.cashq.core.sharing.CashCountShareData?>(null) }
    var showShareFormatDialog by remember { mutableStateOf(false) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearNotification()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CashQTopAppBar(
                title = "CashQ",
                subtitle = "Count History",
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Filter
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search by title...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                EmptyHistoryState(isSearching = uiState.searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionItemCard(
                            session = session,
                            onOpenDetails = { viewModel.openDetails(session) },
                            onShare = {
                                shareDataToShare = viewModel.createShareData(session)
                                showShareFormatDialog = true
                            },
                            onLoad = { onLoadSessionIntoCounter(session) },
                            onDelete = { viewModel.requestDelete(session) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("Delete Session?") },
            text = {
                Text("Are you sure you want to delete \"${session.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Details breakdown dialog
    uiState.selectedSessionForDetails?.let { session ->
        SessionBreakdownDialog(
            session = session,
            onDismiss = { viewModel.closeDetails() },
            onShare = {
                shareDataToShare = viewModel.createShareData(session)
                showShareFormatDialog = true
            }
        )
    }

    // Share Format Selection Dialog
    shareDataToShare?.let { data ->
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

@Composable
fun SessionItemCard(
    session: SavedSessionEntity,
    onOpenDetails: () -> Unit,
    onShare: () -> Unit,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.forLanguageTag("en-IN"))
    }
    val dateString = remember(session.createdAt) {
        dateFormat.format(Date(session.createdAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Total Rupees
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatPaise(session.totalPaise),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${CurrencyFormatter.formatUnits(session.totalUnits)} units",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (session.isNotesOnly) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Notes only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenDetails) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Breakdown")
                }

                TextButton(onClick = onLoad) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load")
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState(
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "No matching sessions found" else "No saved counts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearching)
                    "Try a different search keyword."
                else
                    "Count your physical currency and tap 'Save' on the counter screen to store your count history here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SessionBreakdownDialog(
    session: SavedSessionEntity,
    onDismiss: () -> Unit,
    onShare: () -> Unit = {}
) {
    val quantities = remember(session) { session.getQuantitiesMap() }
    val mode = remember(session.coinMode) {
        try {
            CoinCountingMode.valueOf(session.coinMode)
        } catch (_: Exception) {
            CoinCountingMode.SEPARATE
        }
    }
    val visibleDenoms = remember(session) {
        CashCalculationEngine.getVisibleDenominations(session.coinsVisible, mode)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(session.title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = "Total: ${CurrencyFormatter.formatPaise(session.totalPaise)} (${CurrencyFormatter.formatUnits(session.totalUnits)} units)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (session.isNotesOnly) {
                        Text(
                            text = "Mode: Banknotes Only (Coins hidden)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(visibleDenoms) { denom ->
                    val qty = quantities[denom.id] ?: 0
                    if (qty > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${denom.label} × $qty",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = CurrencyFormatter.formatRupees(denom.valueInRupees * qty),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                onDismiss()
                onShare()
            }) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }
    )
}
