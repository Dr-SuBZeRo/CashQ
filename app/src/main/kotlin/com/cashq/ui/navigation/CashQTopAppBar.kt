package com.cashq.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cashq.R

/**
 * Branded CashQ wordmark displaying "Cash" with an accented brand blue "Q".
 * Dynamically adapts to dark and light backgrounds.
 */
@Composable
fun CashQWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
) {
    val cashColor = if (isDark) Color.White else Color(0xFF0F172A)
    val qColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = cashColor, fontWeight = FontWeight.Bold)) {
                append("Cash")
            }
            withStyle(SpanStyle(color = qColor, fontWeight = FontWeight.ExtraBold)) {
                append("Q")
            }
        },
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/**
 * Shared top app bar for CashQ primary screens (Counter, History, Settings).
 * Displays a menu button for the navigation drawer, CashQ logo, styled wordmark, and subtitle.
 *
 * In dark theme: Uses near-black surface with white Cash and blue Q.
 * In light theme: Uses clean light surface with dark Cash and blue Q.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashQTopAppBar(
    title: String = "CashQ",
    subtitle: String? = "Indian Rupee Cash Counter",
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val iconColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dividerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(R.drawable.cashq_logo),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (title == "CashQ") {
                            CashQWordmark(
                                style = MaterialTheme.typography.titleLarge,
                                isDark = isDark
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = iconColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = iconColor
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor,
                titleContentColor = iconColor,
                navigationIconContentColor = iconColor,
                actionIconContentColor = iconColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(color = dividerColor, thickness = 1.dp)
    }
}
