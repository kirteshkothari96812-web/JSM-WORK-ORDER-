package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.BillingType
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PrintType
import com.example.data.model.Priority
import com.example.data.model.RateUnit
import com.example.data.model.WorkOrder
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
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
import com.example.util.SmartSizeFormatter
import java.util.UUID

private data class DraftOrderItem(
    val id: String = UUID.randomUUID().toString(),
    val sizeValue: TextFieldValue = TextFieldValue(""),
    val widthPart: String = "",
    val heightPart: String = "",
    val useTwoPartSize: Boolean = false,
    val printType: PrintType = PrintType.PLAIN,
    val rateUnit: RateUnit = RateUnit.PER_BAG,
    val quantityText: String = "",
    val rateText: String = ""
) {
    val quantityInt: Int get() = quantityText.trim().toIntOrNull() ?: 0
    val rateDouble: Double get() = rateText.trim().toDoubleOrNull() ?: 0.0
    val estimatedAmount: Double
        get() = if (rateUnit == RateUnit.PER_BAG) {
            quantityInt * rateDouble
        } else {
            0.0
        }
}

@Composable
fun NewOrderScreen(
    settings: AppSettings,
    partySuggestions: List<String>,
    editingOrder: WorkOrder?,
    onCancelEdit: () -> Unit,
    onSaveOrder: (WorkOrder, isEdit: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var partyName by remember { mutableStateOf("") }
    var orderDateIso by remember { mutableStateOf(DateUtils.todayIso()) }
    var deliveryDaysText by remember { mutableStateOf("3") }
    var priority by remember { mutableStateOf(Priority.MID) }
    var takenBy by remember { mutableStateOf(settings.partnerOneName) }
    var billingType by remember { mutableStateOf(BillingType.NON_GST) }
    var taxPercent by remember { mutableStateOf(0.0) }
    var isCustomTax by remember { mutableStateOf(false) }
    var customTaxText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val draftItems = remember { mutableStateListOf(DraftOrderItem()) }
    val presetPercentages = listOf(0.0, 9.0, 14.0, 18.0)

    LaunchedEffect(settings.partnerOneName, settings.partnerTwoName, editingOrder) {
        if (editingOrder == null && takenBy != settings.partnerOneName && takenBy != settings.partnerTwoName) {
            takenBy = settings.partnerOneName
        }
    }

    LaunchedEffect(editingOrder) {
        if (editingOrder != null) {
            partyName = editingOrder.partyName
            orderDateIso = editingOrder.orderDate.ifBlank { DateUtils.todayIso() }
            deliveryDaysText = editingOrder.deliveryDays.toString()
            priority = editingOrder.priorityEnum
            takenBy = editingOrder.takenBy.ifBlank { settings.partnerOneName }
            billingType = editingOrder.billingTypeEnum
            taxPercent = if (editingOrder.billingTypeEnum == BillingType.GST && editingOrder.taxPercent == 0.0) {
                18.0
            } else {
                editingOrder.taxPercent
            }
            val matchesPreset = presetPercentages.any { kotlin.math.abs(it - taxPercent) < 0.001 }
            isCustomTax = billingType == BillingType.NON_GST && !matchesPreset
            customTaxText = if (isCustomTax) {
                if (taxPercent % 1.0 == 0.0) taxPercent.toInt().toString() else taxPercent.toString()
            } else {
                ""
            }
            draftItems.clear()
            if (editingOrder.items.isEmpty()) {
                draftItems.add(DraftOrderItem())
            } else {
                editingOrder.items.forEach { item ->
                    val parts = item.size.split("×").map { it.trim() }
                    draftItems.add(
                        DraftOrderItem(
                            id = item.id,
                            sizeValue = TextFieldValue(item.size, TextRange(item.size.length)),
                            widthPart = parts.getOrElse(0) { "" },
                            heightPart = parts.getOrElse(1) { "" },
                            printType = PrintType.fromString(item.printType),
                            rateUnit = item.rateUnitEnum,
                            quantityText = if (item.effectiveOrderedQty > 0) item.effectiveOrderedQty.toString() else "",
                            rateText = if (item.rate > 0.0) {
                                if (item.rate % 1.0 == 0.0) item.rate.toInt().toString() else item.rate.toString()
                            } else ""
                        )
                    )
                }
            }
            validationError = null
        }
    }

    val deliveryDays = deliveryDaysText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    val expectedDeliveryIso = DateUtils.addDaysIso(orderDateIso, deliveryDays)
    val resolvedTaxPercent = when {
        billingType == BillingType.GST -> 18.0
        isCustomTax -> (customTaxText.trim().toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        else -> taxPercent
    }
    val hasPerKgsItems = draftItems.any { it.rateUnit == RateUnit.PER_KGS }
    val subtotal = draftItems.sumOf { it.estimatedAmount }
    val appliedTaxAmount = subtotal * (resolvedTaxPercent / 100.0)
    val finalGrandTotal = CurrencyFormatter.roundOff(subtotal + appliedTaxAmount)

    val matchingSuggestions = remember(partyName, partySuggestions) {
        if (partyName.isBlank()) {
            partySuggestions.take(5)
        } else {
            partySuggestions.filter {
                it.contains(partyName.trim(), ignoreCase = true) &&
                    !it.equals(partyName.trim(), ignoreCase = true)
            }.take(5)
        }
    }

    if (showDatePicker) {
        IsoDatePickerDialog(
            initialIso = orderDateIso,
            onDateSelected = { picked -> orderDateIso = picked },
            onDismiss = { showDatePicker = false }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (editingOrder != null) {
            item(key = "edit_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Editing: ${editingOrder.partyName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = {
                            onCancelEdit()
                            partyName = ""
                            orderDateIso = DateUtils.todayIso()
                            deliveryDaysText = "3"
                            priority = Priority.MID
                            takenBy = settings.partnerOneName
                            billingType = BillingType.NON_GST
                            taxPercent = 0.0
                            isCustomTax = false
                            customTaxText = ""
                            draftItems.clear()
                            draftItems.add(DraftOrderItem())
                        }) {
                            Text("Cancel Edit")
                        }
                    }
                }
            }
        }

        // Party Name + Autocomplete Suggestions
        item(key = "party_section") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Party & Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = partyName,
                        onValueChange = {
                            partyName = it
                            validationError = null
                        },
                        label = { Text("Party / Customer Name *") },
                        placeholder = { Text("e.g. Shree Ganesh Agro") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("party_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (matchingSuggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            matchingSuggestions.forEach { suggestion ->
                                FilterChip(
                                    selected = false,
                                    onClick = { partyName = suggestion },
                                    label = {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // Order Date + Delivery Days Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Order Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable { showDatePicker = true }
                                    .testTag("order_date_picker_button"),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = "Select Order Date",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = DateUtils.formatDisplayDate(orderDateIso),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(0.85f)) {
                            OutlinedTextField(
                                value = deliveryDaysText,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 3) {
                                        deliveryDaysText = input
                                    }
                                },
                                label = { Text("Delivery (Days)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("delivery_days_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Quick Delivery Days Chips + Expected Delivery Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 5, 7).forEach { days ->
                                val isSel = deliveryDaysText == days.toString()
                                FilterChip(
                                    selected = isSel,
                                    onClick = { deliveryDaysText = days.toString() },
                                    label = { Text("${days}d") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EXPECTED DELIVERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateUtils.formatDisplayDate(expectedDeliveryIso),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Priority Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Priority.entries.forEach { option ->
                                PriorityPillButton(
                                    priority = option,
                                    selected = priority == option,
                                    onClick = { priority = option },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Order Taken By Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Order Taken By",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val partners = listOf(settings.partnerOneName, settings.partnerTwoName).distinct()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            partners.forEach { partner ->
                                val selected = takenBy.equals(partner, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { takenBy = partner }
                                        .testTag("taken_by_$partner"),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = partner,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Items Header
        item(key = "items_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bag Sizes & Items (${draftItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tip: type 19/ or 19 space → 19×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Multi-Item Rows
        itemsIndexed(items = draftItems, key = { _, item -> item.id }) { index, item ->
            DraftItemCard(
                index = index,
                item = item,
                canRemove = draftItems.size > 1,
                onUpdate = { updated ->
                    draftItems[index] = updated
                    validationError = null
                },
                onRemove = {
                    if (draftItems.size > 1) {
                        draftItems.removeAt(index)
                    }
                }
            )
        }

        // Add Another Size/Item Button
        item(key = "add_item_btn") {
            OutlinedButton(
                onClick = { draftItems.add(DraftOrderItem()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_another_item_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_add_item),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Billing Type & Percentage Section
        item(key = "billing_tax_section") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Billing & Tax",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Billing Type Toggle: GST / Non-GST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillingType.entries.forEach { type ->
                            val selected = billingType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        billingType = type
                                        if (type == BillingType.GST) {
                                            isCustomTax = false
                                            taxPercent = 18.0
                                        } else {
                                            isCustomTax = false
                                            taxPercent = 0.0
                                        }
                                    }
                                    .testTag("billing_type_${type.name.lowercase()}"),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = type.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    if (billingType == BillingType.GST) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gst_default_18_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GST Tax Rate (Default)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "18%",
                                    style = MonoMetricStyle,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        // Tax / Percentage (%) Quick-Select Pills + Custom % for Non-GST
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Percentage (%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetPercentages.forEach { pct ->
                                    val selected = !isCustomTax && kotlin.math.abs(taxPercent - pct) < 0.001
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            isCustomTax = false
                                            taxPercent = pct
                                        },
                                        label = {
                                            Text(
                                                text = CurrencyFormatter.formatPercent(pct),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        modifier = Modifier.testTag("tax_pill_${pct.toInt()}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }

                                FilterChip(
                                    selected = isCustomTax,
                                    onClick = {
                                        isCustomTax = true
                                        if (customTaxText.isBlank() && resolvedTaxPercent > 0.0) {
                                            customTaxText = if (resolvedTaxPercent % 1.0 == 0.0) {
                                                resolvedTaxPercent.toInt().toString()
                                            } else {
                                                resolvedTaxPercent.toString()
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = "Custom %",
                                            fontWeight = if (isCustomTax) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.testTag("tax_pill_custom"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }

                            if (isCustomTax) {
                                OutlinedTextField(
                                    value = customTaxText,
                                    onValueChange = { input ->
                                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$")) && input.length <= 6) {
                                            customTaxText = input
                                        }
                                    },
                                    label = { Text("Custom Percentage (%)") },
                                    placeholder = { Text("e.g. 5 or 12.5") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_tax_percent_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Validation Error + Subtotal / Applied % / Final Grand Total & Save Button
        item(key = "save_footer") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (validationError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = validationError!!,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ordered Bags (${draftItems.sumOf { it.quantityInt }})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = if (hasPerKgsItems && subtotal == 0.0) "Wt. at Dispatch" else CurrencyFormatter.formatInr(subtotal),
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${billingType.label} (${CurrencyFormatter.formatPercent(resolvedTaxPercent)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                            Text(
                                text = if (hasPerKgsItems && subtotal == 0.0) "On Dispatch Total" else "+ ${CurrencyFormatter.formatInr(appliedTaxAmount)}",
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (hasPerKgsItems) "FINAL TOTAL" else "EST. GRAND TOTAL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = if (hasPerKgsItems) "Calculated at Dispatch" else CurrencyFormatter.formatInr(finalGrandTotal),
                                style = if (hasPerKgsItems) MaterialTheme.typography.titleSmall else MonoMetricStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("final_grand_total_text")
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val trimmedParty = partyName.trim()
                        if (trimmedParty.isEmpty()) {
                            validationError = "Please enter the Party / Customer Name."
                            return@Button
                        }
                        val validItems = draftItems.map { draft ->
                            val cleanSize = if (draft.useTwoPartSize) {
                                "${draft.widthPart.trim()}×${draft.heightPart.trim()}".trim(' ', '×')
                            } else {
                                draft.sizeValue.text.trim()
                            }
                            OrderItem(
                                id = draft.id,
                                size = cleanSize,
                                printType = draft.printType.label,
                                rateUnit = draft.rateUnit.label,
                                orderedQuantity = draft.quantityInt,
                                quantity = draft.quantityInt,
                                weightKg = 0.0,
                                rate = draft.rateDouble,
                                billingType = billingType.label,
                                taxPercent = resolvedTaxPercent
                            )
                        }
                        if (validItems.any { it.size.isBlank() }) {
                            validationError = "Please enter bag size (e.g. 19×32.5) for all items."
                            return@Button
                        }
                        if (validItems.any { it.quantity <= 0 || it.rate <= 0.0 }) {
                            validationError = "Please enter valid Ordered Bags and Rate (₹) for all items."
                            return@Button
                        }

                        val computedSubtotal = validItems.sumOf { it.amount }
                        val finalTotal = CurrencyFormatter.roundOff(
                            computedSubtotal + (computedSubtotal * (resolvedTaxPercent / 100.0))
                        )
                        val orderToSave = WorkOrder(
                            id = editingOrder?.id ?: UUID.randomUUID().toString(),
                            partyName = trimmedParty,
                            orderDate = orderDateIso,
                            deliveryDays = deliveryDays,
                            expectedDeliveryDate = expectedDeliveryIso,
                            priority = priority.label,
                            takenBy = takenBy,
                            items = validItems,
                            totalAmount = finalTotal,
                            status = editingOrder?.status ?: OrderStatus.PENDING.value,
                            completionDate = editingOrder?.completionDate,
                            creditDays = editingOrder?.creditDays ?: 5,
                            paymentDueDate = editingOrder?.paymentDueDate,
                            paidAmount = editingOrder?.paidAmount ?: 0.0,
                            updatedAt = System.currentTimeMillis(),
                            billingType = billingType.label,
                            taxPercent = resolvedTaxPercent,
                            itemsSummary = WorkOrder.buildItemsSummary(
                                items = validItems,
                                billingType = billingType.label,
                                taxPercent = resolvedTaxPercent
                            )
                        )
                        onSaveOrder(orderToSave, editingOrder != null)

                        // Reset form after saving new order
                        partyName = ""
                        orderDateIso = DateUtils.todayIso()
                        deliveryDaysText = "3"
                        priority = Priority.MID
                        takenBy = settings.partnerOneName
                        billingType = BillingType.NON_GST
                        taxPercent = 0.0
                        isCustomTax = false
                        customTaxText = ""
                        draftItems.clear()
                        draftItems.add(DraftOrderItem())
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_work_order_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = if (editingOrder != null) {
                            stringResource(R.string.action_update_order)
                        } else {
                            stringResource(R.string.action_save_order)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PriorityPillButton(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (selectedBg, unselectedBg, textColor, border) = when (priority) {
        Priority.HIGH -> listOf(
            PriorityHighRed,
            if (isDark) PriorityHighBgDark else PriorityHighBgLight,
            if (selected) Color.White else if (isDark) PriorityHighTextDark else PriorityHighRed,
            PriorityHighRed
        )
        Priority.MID -> listOf(
            PriorityMidAmber,
            if (isDark) PriorityMidBgDark else PriorityMidBgLight,
            if (selected) Color.White else if (isDark) PriorityMidTextDark else PriorityMidAmber,
            PriorityMidAmber
        )
        Priority.LOW -> listOf(
            PriorityLowGreen,
            if (isDark) PriorityLowBgDark else PriorityLowBgLight,
            if (selected) Color.White else if (isDark) PriorityLowTextDark else PriorityLowGreen,
            PriorityLowGreen
        )
    }

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) border else border.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("priority_pill_${priority.label.lowercase()}"),
        color = if (selected) selectedBg else unselectedBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun DraftItemCard(
    index: Int,
    item: DraftOrderItem,
    canRemove: Boolean,
    onUpdate: (DraftOrderItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Item #${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val nextTwoPart = !item.useTwoPartSize
                                if (nextTwoPart) {
                                    val parts = item.sizeValue.text.split("×").map { it.trim() }
                                    onUpdate(
                                        item.copy(
                                            useTwoPartSize = true,
                                            widthPart = parts.getOrElse(0) { "" },
                                            heightPart = parts.getOrElse(1) { "" }
                                        )
                                    )
                                } else {
                                    val joined = if (item.widthPart.isNotBlank() && item.heightPart.isNotBlank()) {
                                        "${item.widthPart.trim()}×${item.heightPart.trim()}"
                                    } else {
                                        item.widthPart.trim()
                                    }
                                    onUpdate(
                                        item.copy(
                                            useTwoPartSize = false,
                                            sizeValue = TextFieldValue(joined, TextRange(joined.length))
                                        )
                                    )
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (item.useTwoPartSize) "Single Box" else "W × H Split",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (canRemove) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove item",
                                tint = PriorityHighRed
                            )
                        }
                    }
                }
            }

            // Smart Size Input (Auto-replace /, x, X, space -> "×" directly OR 2-part numeric W×H)
            if (!item.useTwoPartSize) {
                OutlinedTextField(
                    value = item.sizeValue,
                    onValueChange = { incoming ->
                        val formatted = SmartSizeFormatter.applySmartFormatting(item.sizeValue, incoming)
                        onUpdate(item.copy(sizeValue = formatted))
                    },
                    label = { Text("Bag Size (e.g. 19×32.5) *") },
                    placeholder = { Text("Type 19/32.5 or 19 32.5") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    trailingIcon = {
                        if (item.sizeValue.text.isNotEmpty()) {
                            IconButton(onClick = {
                                onUpdate(item.copy(sizeValue = TextFieldValue("")))
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear size")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_size_input_$index"),
                    shape = RoundedCornerShape(12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.widthPart,
                        onValueChange = { w ->
                            if (w.matches(Regex("^\\d*\\.?\\d*$"))) {
                                val combined = if (w.isNotBlank() && item.heightPart.isNotBlank()) {
                                    "$w×${item.heightPart}"
                                } else w
                                onUpdate(
                                    item.copy(
                                        widthPart = w,
                                        sizeValue = TextFieldValue(combined, TextRange(combined.length))
                                    )
                                )
                            }
                        },
                        label = { Text("Width") },
                        placeholder = { Text("19") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = item.heightPart,
                        onValueChange = { h ->
                            if (h.matches(Regex("^\\d*\\.?\\d*$"))) {
                                val combined = if (item.widthPart.isNotBlank() && h.isNotBlank()) {
                                    "${item.widthPart}×$h"
                                } else item.widthPart
                                onUpdate(
                                    item.copy(
                                        heightPart = h,
                                        sizeValue = TextFieldValue(combined, TextRange(combined.length))
                                    )
                                )
                            }
                        },
                        label = { Text("Length") },
                        placeholder = { Text("32.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Print Type Selector (Plain / Single Print / Double Side Print)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Print Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrintType.entries.forEach { pt ->
                        val selected = item.printType == pt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onUpdate(item.copy(printType = pt)) }
                                .testTag("print_type_${index}_${pt.name.lowercase()}"),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = pt.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Rate Unit Toggle: Per Bag vs Per Kgs
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Rate Basis",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RateUnit.entries.forEach { unit ->
                        val selected = item.rateUnit == unit
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onUpdate(item.copy(rateUnit = unit)) }
                                .testTag("rate_unit_${index}_${unit.name.lowercase()}"),
                            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unit.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Ordered Qty (Bags) & Rate Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = item.quantityText,
                    onValueChange = { q ->
                        if (q.all { it.isDigit() } && q.length <= 7) {
                            onUpdate(item.copy(quantityText = q))
                        }
                    },
                    label = { Text("Order Qty (Bags) *") },
                    placeholder = { Text("1000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_qty_input_$index"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = item.rateText,
                    onValueChange = { r ->
                        if (r.matches(Regex("^\\d*\\.?\\d{0,2}$")) && r.length <= 9) {
                            onUpdate(item.copy(rateText = r))
                        }
                    },
                    label = {
                        Text(
                            if (item.rateUnit == RateUnit.PER_KGS) "Rate (₹ / Kg) *" else "Rate (₹ / Bag) *"
                        )
                    },
                    placeholder = {
                        Text(if (item.rateUnit == RateUnit.PER_KGS) "115" else "42.5")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_rate_input_$index"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Item Strip (Per Bag estimated total vs Per Kgs weight-at-dispatch indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.rateUnit == RateUnit.PER_KGS) {
                        "Final weight & total entered at Dispatch"
                    } else {
                        "Est. Item Amount (final bags at Dispatch)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (item.rateUnit == RateUnit.PER_KGS) {
                        "${CurrencyFormatter.formatRateInr(item.rateDouble)}/kg"
                    } else {
                        CurrencyFormatter.formatInr(item.estimatedAmount)
                    },
                    style = MonoMetricStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
