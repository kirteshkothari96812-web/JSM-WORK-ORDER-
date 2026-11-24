package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.model.SyncStatus
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialAmberBgDark
import com.example.ui.theme.IndustrialAmberBgLight
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.PriorityHighBgDark
import com.example.ui.theme.PriorityHighBgLight
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityHighTextDark
import com.example.ui.theme.PriorityLowBgDark
import com.example.ui.theme.PriorityLowBgLight
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityLowTextDark
import com.example.ui.theme.PriorityMidAmber
import com.example.ui.theme.PriorityMidBgDark
import com.example.ui.theme.PriorityMidBgLight
import com.example.ui.theme.PriorityMidTextDark
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun PriorityBadge(priority: Priority, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg, dot) = when (priority) {
        Priority.HIGH -> Triple(
            if (isDark) PriorityHighBgDark else PriorityHighBgLight,
            if (isDark) PriorityHighTextDark else PriorityHighRed,
            PriorityHighRed
        )
        Priority.MID -> Triple(
            if (isDark) PriorityMidBgDark else PriorityMidBgLight,
            if (isDark) PriorityMidTextDark else PriorityMidAmber,
            PriorityMidAmber
        )
        Priority.LOW -> Triple(
            if (isDark) PriorityLowBgDark else PriorityLowBgLight,
            if (isDark) PriorityLowTextDark else PriorityLowGreen,
            PriorityLowGreen
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = priority.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg) = when (status) {
        PaymentStatus.UNPAID -> Pair(
            if (isDark) PriorityHighBgDark else PriorityHighBgLight,
            if (isDark) PriorityHighTextDark else PriorityHighRed
        )
        PaymentStatus.PARTIAL -> Pair(
            if (isDark) PriorityMidBgDark else PriorityMidBgLight,
            if (isDark) PriorityMidTextDark else PriorityMidAmber
        )
        PaymentStatus.PAID -> Pair(
            if (isDark) PriorityLowBgDark else PriorityLowBgLight,
            if (isDark) PriorityLowTextDark else PriorityLowGreen
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun BillingBadge(
    billingType: com.example.data.model.BillingType,
    taxPercent: Double,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isGst = billingType == com.example.data.model.BillingType.GST
    val bg = when {
        isGst && isDark -> Color(0xFF172554)
        isGst -> Color(0xFFDBEAFE)
        isDark -> Color(0xFF1E293B)
        else -> Color(0xFFE2E8F0)
    }
    val fg = when {
        isGst && isDark -> Color(0xFF93C5FD)
        isGst -> Color(0xFF1D4ED8)
        isDark -> Color(0xFFCBD5E1)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${billingType.label} • ${CurrencyFormatter.formatPercent(taxPercent)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
fun SyncStatusPill(
    syncStatus: SyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (bg, fg, label) = when (syncStatus) {
        is SyncStatus.Syncing -> Triple(
            Color(0xFF1E293B),
            Color(0xFF93C5FD),
            "Syncing…"
        )
        is SyncStatus.Synced -> Triple(
            if (isDark) PriorityLowBgDark else Color(0xFF064E3B),
            Color(0xFF6EE7B7),
            if (syncStatus.pendingQueueCount > 0) "Queued (${syncStatus.pendingQueueCount})" else "Synced"
        )
        is SyncStatus.Error -> Triple(
            Color(0xFF7F1D1D),
            Color(0xFFFCA5A5),
            "Retry Sync"
        )
        is SyncStatus.NotConfigured -> Triple(
            Color(0xFF334155),
            Color(0xFFFDE68A),
            "Setup Sheet"
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag("sync_status_pill"),
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (syncStatus) {
                is SyncStatus.Syncing -> CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = fg
                )
                is SyncStatus.Synced -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Synced",
                    tint = fg,
                    modifier = Modifier.size(13.dp)
                )
                is SyncStatus.Error -> Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Sync error",
                    tint = fg,
                    modifier = Modifier.size(13.dp)
                )
                is SyncStatus.NotConfigured -> Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = "Sheet not configured",
                    tint = fg,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TopReminderAlertBanner(
    dueCount: Int,
    totalPendingDue: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) PriorityHighBgDark else PriorityHighBgLight
    val border = if (isDark) PriorityHighRed.copy(alpha = 0.6f) else PriorityHighRed.copy(alpha = 0.35f)
    val accent = if (isDark) PriorityHighTextDark else PriorityHighRed

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("top_reminder_alert_banner"),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PriorityHighRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "Payment Due Alert",
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (dueCount == 1) "1 Payment Reminder Due" else "$dueCount Payment Reminders Due",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = "${CurrencyFormatter.formatInr(totalPendingDue)} due or overdue — Tap to view reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open reminders",
                tint = accent
            )
        }
    }
}

@Composable
fun PartySearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    filterLabels: List<String>,
    selectedIndex: Int,
    onSelectFilter: (Int) -> Unit,
    modifier: Modifier = Modifier,
    searchTag: String = "party_search_input"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(searchTag),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterLabels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(index) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsoDatePickerDialog(
    initialIso: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = DateUtils.isoToMillis(initialIso) ?: System.currentTimeMillis()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = state.selectedDateMillis
                    if (picked != null) {
                        onDateSelected(DateUtils.millisToIso(picked))
                    }
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun MetricPillBox(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoMetricStyle,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
