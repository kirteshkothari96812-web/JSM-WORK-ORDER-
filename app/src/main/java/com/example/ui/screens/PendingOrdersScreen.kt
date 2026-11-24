package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Priority
import com.example.data.model.WorkOrder
import com.example.ui.components.PartySearchAndFilterBar
import com.example.ui.components.PriorityBadge
import com.example.ui.components.TopReminderAlertBanner
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialAmberBgDark
import com.example.ui.theme.IndustrialAmberBgLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.PriorityHighBgDark
import com.example.ui.theme.PriorityHighBgLight
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityHighTextDark
import com.example.ui.viewmodel.ReminderOrderUi
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun PendingOrdersScreen(
    orders: List<WorkOrder>,
    reminderOrders: List<ReminderOrderUi>,
    searchQuery: String,
    selectedPriority: Priority?,
    onSearchChange: (String) -> Unit,
    onPriorityFilterChange: (Priority?) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenNewOrder: () -> Unit,
    onEditOrder: (WorkOrder) -> Unit,
    onDeleteOrder: (WorkOrder) -> Unit,
    onMarkComplete: (WorkOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterOptions = listOf("All", "High", "Mid", "Low")
    val selectedIndex = when (selectedPriority) {
        null -> 0
        Priority.HIGH -> 1
        Priority.MID -> 2
        Priority.LOW -> 3
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (reminderOrders.isNotEmpty()) {
            item(key = "reminder_banner") {
                val totalDue = reminderOrders.sumOf { it.order.pendingBalance }
                TopReminderAlertBanner(
                    dueCount = reminderOrders.size,
                    totalPendingDue = totalDue,
                    onClick = onOpenReminders
                )
            }
        }

        item(key = "search_filter_bar") {
            PartySearchAndFilterBar(
                query = searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "Search party name...",
                filterLabels = filterOptions,
                selectedIndex = selectedIndex,
                onSelectFilter = { idx ->
                    onPriorityFilterChange(
                        when (idx) {
                            1 -> Priority.HIGH
                            2 -> Priority.MID
                            3 -> Priority.LOW
                            else -> null
                        }
                    )
                },
                searchTag = "pending_search_input"
            )
        }

        item(key = "count_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${orders.size} Active Work Order${if (orders.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalValue = orders.sumOf { it.totalAmount }
                Text(
                    text = CurrencyFormatter.formatInr(totalValue),
                    style = MonoMetricStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (orders.isEmpty()) {
            item(key = "empty_state") {
                EmptyPendingOrdersCard(
                    hasFilter = searchQuery.isNotBlank() || selectedPriority != null,
                    onCreateOrder = onOpenNewOrder
                )
            }
        } else {
            items(items = orders, key = { it.id }) { order ->
                PendingOrderCard(
                    order = order,
                    onEdit = { onEditOrder(order) },
                    onDelete = { onDeleteOrder(order) },
                    onMarkComplete = { onMarkComplete(order) }
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PendingOrderCard(
    order: WorkOrder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkComplete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val todayIso = DateUtils.todayIso()
    val daysToDelivery = if (order.expectedDeliveryDate.isNotBlank()) {
        DateUtils.daysBetweenIso(todayIso, order.expectedDeliveryDate)
    } else {
        1
    }
    val isOverdue = daysToDelivery < 0
    val isDueToday = daysToDelivery == 0

    val borderColor = when {
        isOverdue -> PriorityHighRed.copy(alpha = 0.65f)
        order.priorityEnum == Priority.HIGH -> PriorityHighRed.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_order_card_${order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header Row: Priority + Taken By + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityBadge(priority = order.priorityEnum)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Taken by",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = order.takenBy,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit order",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete order",
                            tint = PriorityHighRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Party Name & Billing Badge
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = order.partyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                com.example.ui.components.BillingBadge(
                    billingType = order.billingTypeEnum,
                    taxPercent = order.taxPercent
                )
            }

            // Dates Strip: Order Date & Expected Delivery Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isOverdue -> if (isDark) PriorityHighBgDark else PriorityHighBgLight
                            isDueToday -> if (isDark) IndustrialAmberBgDark else IndustrialAmberBgLight
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER DATE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDisplayDate(order.orderDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val deliveryLabel = when {
                        isOverdue -> "OVERDUE (${-daysToDelivery}d)"
                        isDueToday -> "DUE TODAY"
                        else -> "DELIVERY (${order.deliveryDays}d)"
                    }
                    val deliveryColor = when {
                        isOverdue -> if (isDark) PriorityHighTextDark else PriorityHighRed
                        isDueToday -> IndustrialAmber
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = deliveryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = deliveryColor
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isOverdue) deliveryColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DateUtils.formatDisplayDate(order.expectedDeliveryDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) deliveryColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bag Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                order.items.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.size.ifBlank { "Custom Size" },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• ${item.printType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val isUnweighedPerKgs =
                                item.rateUnitEnum == com.example.data.model.RateUnit.PER_KGS &&
                                    item.weightKg <= 0.0
                            val itemSummaryLine = when {
                                isUnweighedPerKgs -> {
                                    "${item.effectiveOrderedQty} bags • ${CurrencyFormatter.formatRateInr(item.rate)}/kg"
                                }
                                item.rateUnitEnum == com.example.data.model.RateUnit.PER_KGS -> {
                                    val bagPart = if (item.quantity > 0) " (${item.quantity} bags)" else ""
                                    "${CurrencyFormatter.formatWeight(item.weightKg)} kg × ${CurrencyFormatter.formatRateInr(item.rate)}/kg$bagPart"
                                }
                                else -> {
                                    "${item.effectiveOrderedQty} bags × ${CurrencyFormatter.formatRateInr(item.rate)}/bag"
                                }
                            }
                            Text(
                                text = itemSummaryLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val isUnweighed =
                            item.rateUnitEnum == com.example.data.model.RateUnit.PER_KGS &&
                                item.weightKg <= 0.0
                        Text(
                            text = if (isUnweighed) "Wt. at dispatch" else CurrencyFormatter.formatInr(item.amount),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            style = if (isUnweighed) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                            color = if (isUnweighed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Grand Total + Mark Complete / Dispatch Button
            val hasUnweighedPerKgs = order.items.any {
                it.rateUnitEnum == com.example.data.model.RateUnit.PER_KGS && it.weightKg <= 0.0
            }
            val orderedBagsSum = order.items.sumOf { it.effectiveOrderedQty }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasUnweighedPerKgs) {
                            "ORDER TOTAL ($orderedBagsSum bags)"
                        } else {
                            "GRAND TOTAL ($orderedBagsSum bags)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasUnweighedPerKgs) {
                        Text(
                            text = "Final bill calculated after dispatch weight",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (hasUnweighedPerKgs) {
                        if (order.totalAmount > 0.0) "Est. ${CurrencyFormatter.formatInr(order.totalAmount)}" else "At Dispatch"
                    } else {
                        CurrencyFormatter.formatInr(order.totalAmount)
                    },
                    style = MonoMetricStyle.copy(
                        fontSize = if (hasUnweighedPerKgs) MaterialTheme.typography.titleMedium.fontSize else MaterialTheme.typography.titleLarge.fontSize
                    ),
                    color = if (hasUnweighedPerKgs) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = onMarkComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("mark_complete_button_${order.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_mark_complete),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyPendingOrdersCard(
    hasFilter: Boolean,
    onCreateOrder: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_orders),
                contentDescription = "No pending work orders",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (hasFilter) "No matching work orders" else "All Caught Up!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasFilter) {
                    "Try clearing your party search or priority filter."
                } else {
                    "Add a new factory work order or tap Sync to pull orders from Google Sheets."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasFilter) {
                Button(
                    onClick = onCreateOrder,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_new_order_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create Work Order")
                }
            }
        }
    }
}
