package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PaymentStatus
import com.example.data.model.WorkOrder
import com.example.ui.components.MetricPillBox
import com.example.ui.components.PartySearchAndFilterBar
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityHighTextDark
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityLowTextDark
import com.example.ui.viewmodel.PaymentFilter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun CompletedOrdersScreen(
    orders: List<WorkOrder>,
    totalDispatchedAmount: Double,
    totalPendingReceivable: Double,
    searchQuery: String,
    selectedFilter: PaymentFilter,
    onSearchChange: (String) -> Unit,
    onFilterChange: (PaymentFilter) -> Unit,
    onRecordPayment: (WorkOrder) -> Unit,
    onDeleteOrder: (WorkOrder) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val filterLabels = PaymentFilter.entries.map { it.label }
    val selectedIndex = PaymentFilter.entries.indexOf(selectedFilter)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "completed_summary") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPillBox(
                    label = "TOTAL DISPATCHED",
                    value = CurrencyFormatter.formatInr(totalDispatchedAmount),
                    modifier = Modifier.weight(1f)
                )
                MetricPillBox(
                    label = "PENDING BALANCE",
                    value = CurrencyFormatter.formatInr(totalPendingReceivable),
                    valueColor = if (totalPendingReceivable > 0.0) {
                        if (isDark) PriorityHighTextDark else PriorityHighRed
                    } else {
                        if (isDark) PriorityLowTextDark else PriorityLowGreen
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "completed_search_filter") {
            PartySearchAndFilterBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search dispatched party...",
                filterLabels = filterLabels,
                selectedIndex = selectedIndex,
                onSelectFilter = { idx ->
                    onFilterChange(PaymentFilter.entries.getOrElse(idx) { PaymentFilter.ALL })
                },
                searchTag = "completed_search_input"
            )
        }

        if (orders.isEmpty()) {
            item(key = "empty_completed") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No Completed Dispatches",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed work orders will appear here for payment tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(items = orders, key = { it.id }) { order ->
                CompletedOrderCard(
                    order = order,
                    onRecordPayment = { onRecordPayment(order) },
                    onDelete = { onDeleteOrder(order) },
                    onShowMessage = onShowMessage
                )
            }
        }

        item(key = "completed_bottom_spacer") {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CompletedOrderCard(
    order: WorkOrder,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var showItems by remember { mutableStateOf(false) }

    val paymentStatus = order.paymentStatus
    val pendingBalance = order.pendingBalance

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("completed_order_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentStatusBadge(status = paymentStatus)
                    com.example.ui.components.BillingBadge(
                        billingType = order.billingTypeEnum,
                        taxPercent = order.taxPercent
                    )
                    Text(
                        text = "By ${order.takenBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete order",
                        tint = PriorityHighRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = order.partyName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Dispatch Date & Payment Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "DISPATCHED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDisplayDate(order.completionDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PAYMENT DUE (${order.creditDays}d credit)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDisplayDate(order.paymentDueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Financial Breakdown Strip: Total | Paid | Pending Balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(order.totalAmount),
                        style = MonoMetricStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PAID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(order.paidAmount),
                        style = MonoMetricStyle,
                        color = if (isDark) PriorityLowTextDark else PriorityLowGreen
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(pendingBalance),
                        style = MonoMetricStyle,
                        color = if (pendingBalance > 0.01) {
                            if (isDark) PriorityHighTextDark else PriorityHighRed
                        } else {
                            if (isDark) PriorityLowTextDark else PriorityLowGreen
                        }
                    )
                }
            }

            order.paymentDisplayText?.let { payText ->
                if (order.paidAmount > 0.01) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Paid via: $payText",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Expandable Bag Items Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showItems = !showItems }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} bag size(s) • ${order.items.sumOf { it.quantity }} bags",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (showItems) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Toggle items",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showItems) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                ) {
                    order.items.forEachIndexed { idx, item ->
                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val ordNote = if (item.orderedQuantity > 0 && item.orderedQuantity != item.quantity) {
                                " [ord: ${item.orderedQuantity}]"
                            } else {
                                ""
                            }
                            val qtyLabel = if (item.rateUnitEnum == com.example.data.model.RateUnit.PER_KGS) {
                                val bagPart = if (item.quantity > 0) ", ${item.quantity} bags$ordNote" else ""
                                "${CurrencyFormatter.formatWeight(item.weightKg)} kg × ${CurrencyFormatter.formatRateInr(item.rate)}/kg$bagPart"
                            } else {
                                "${item.quantity} bags$ordNote × ${CurrencyFormatter.formatRateInr(item.rate)}/bag"
                            }
                            Text(
                                text = "${item.size} • ${item.printType} ($qtyLabel)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(item.amount),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Record Payment CTA if unpaid/partial
            if (paymentStatus != PaymentStatus.PAID) {
                Button(
                    onClick = onRecordPayment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("record_payment_button_${order.id}"),
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
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_record_payment),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
