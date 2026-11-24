package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WorkOrder
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.PriorityHighBgDark
import com.example.ui.theme.PriorityHighBgLight
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityHighTextDark
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMidAmber
import com.example.ui.theme.PriorityMidBgDark
import com.example.ui.theme.PriorityMidBgLight
import com.example.ui.theme.PriorityMidTextDark
import com.example.ui.viewmodel.ReminderOrderUi
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun RemindersScreen(
    reminders: List<ReminderOrderUi>,
    onRecordPayment: (WorkOrder) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val totalDue = reminders.sumOf { it.order.pendingBalance }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "reminders_header_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "PAYMENT REMINDERS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Text(
                            text = if (reminders.isEmpty()) "All Payments Clear" else "${reminders.size} Party Payment(s) Due",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatInr(totalDue),
                        style = MonoMetricStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (reminders.isEmpty()) {
            item(key = "empty_reminders") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "All payments clear",
                            tint = PriorityLowGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Due or Overdue Payments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed orders within 1 day of their due date or past due will automatically appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = reminders, key = { it.order.id }) { item ->
                ReminderCard(
                    reminder = item,
                    isDark = isDark,
                    onRecordPayment = { onRecordPayment(item.order) },
                    onShowMessage = onShowMessage
                )
            }
        }

        item(key = "reminders_bottom_spacer") {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderOrderUi,
    isDark: Boolean,
    onRecordPayment: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val order = reminder.order
    val days = reminder.daysUntilDue

    val isOverdue = days < 0
    val statusText = when {
        days < 0 -> "Payment ${-days} din overdue hai"
        days == 0 -> "Due Today"
        else -> "Due Tomorrow"
    }

    val (badgeBg, badgeFg, borderColor) = if (isOverdue) {
        Triple(
            if (isDark) PriorityHighBgDark else PriorityHighBgLight,
            if (isDark) PriorityHighTextDark else PriorityHighRed,
            PriorityHighRed.copy(alpha = 0.55f)
        )
    } else {
        Triple(
            if (isDark) PriorityMidBgDark else PriorityMidBgLight,
            if (isDark) PriorityMidTextDark else PriorityMidAmber,
            PriorityMidAmber.copy(alpha = 0.55f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg
                    )
                }
                com.example.ui.components.BillingBadge(
                    billingType = order.billingTypeEnum,
                    taxPercent = order.taxPercent
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.partyName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Dispatched: ${DateUtils.formatDisplayDate(order.completionDate)} • Due: ${DateUtils.formatDisplayDate(order.paymentDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(order.pendingBalance),
                        style = MonoMetricStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                        color = badgeFg
                    )
                }
            }

            order.paymentDisplayText?.let { payText ->
                if (order.paidAmount > 0.01) {
                    Text(
                        text = "Partial paid: $payText",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Record Payment CTA
            Button(
                onClick = onRecordPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reminder_record_payment_button_${order.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Payments,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_record_payment),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
