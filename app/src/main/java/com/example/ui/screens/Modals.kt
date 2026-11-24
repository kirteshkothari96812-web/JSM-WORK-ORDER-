package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.OrderItem
import com.example.data.model.PaymentMode
import com.example.data.model.RateUnit
import com.example.data.model.WorkOrder
import com.example.ui.components.BillingBadge
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.theme.PriorityHighRed
import com.example.util.AppsScriptGuide
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

private data class DispatchItemDraft(
    val sourceItem: OrderItem,
    val actualBagsText: String,
    val actualWeightText: String
) {
    val actualBagsInt: Int
        get() = actualBagsText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

    val actualWeightDouble: Double
        get() = actualWeightText.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    val computedAmount: Double
        get() = if (sourceItem.rateUnitEnum == RateUnit.PER_KGS) {
            actualWeightDouble * sourceItem.rate
        } else {
            actualBagsInt * sourceItem.rate
        }

    fun toDispatchedItem(): OrderItem {
        return sourceItem.copy(
            orderedQuantity = sourceItem.effectiveOrderedQty,
            quantity = actualBagsInt,
            weightKg = if (sourceItem.rateUnitEnum == RateUnit.PER_KGS) actualWeightDouble else 0.0
        )
    }
}

@Composable
fun DispatchOrderDialog(
    order: WorkOrder,
    onDismiss: () -> Unit,
    onConfirmDispatch: (
        dispatchedItems: List<OrderItem>,
        finalTotalAmount: Double,
        completionDateIso: String,
        creditDays: Int
    ) -> Unit
) {
    var dispatchDateIso by remember { mutableStateOf(DateUtils.todayIso()) }
    var creditDaysText by remember { mutableStateOf(order.creditDays.toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val itemDrafts = remember(order.id) {
        mutableStateListOf<DispatchItemDraft>().apply {
            addAll(
                order.items.map { item ->
                    val prefillBags = when {
                        item.quantity > 0 -> item.quantity.toString()
                        item.effectiveOrderedQty > 0 -> item.effectiveOrderedQty.toString()
                        else -> ""
                    }
                    val prefillWeight = if (item.weightKg > 0.0) {
                        CurrencyFormatter.formatWeight(item.weightKg)
                    } else {
                        ""
                    }
                    DispatchItemDraft(
                        sourceItem = item,
                        actualBagsText = prefillBags,
                        actualWeightText = prefillWeight
                    )
                }
            )
        }
    }

    val subtotal = itemDrafts.sumOf { it.computedAmount }
    val taxAmount = subtotal * (order.taxPercent / 100.0)
    val finalGrandTotal = CurrencyFormatter.roundOff(subtotal + taxAmount)

    val creditDays = creditDaysText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
    val paymentDueIso = DateUtils.addDaysIso(dispatchDateIso, creditDays)

    if (showDatePicker) {
        IsoDatePickerDialog(
            initialIso = dispatchDateIso,
            onDateSelected = { dispatchDateIso = it },
            onDismiss = { showDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Dispatch & Complete",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = order.partyName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    BillingBadge(
                        billingType = order.billingTypeEnum,
                        taxPercent = order.taxPercent
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ENTER EXACT DISPATCHED QTY / WEIGHT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                itemDrafts.forEachIndexed { index, draft ->
                    val item = draft.sourceItem
                    val isPerKgs = item.rateUnitEnum == RateUnit.PER_KGS
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.size.ifBlank { "Custom Size" }} • ${item.printType}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (isPerKgs) {
                                        "${CurrencyFormatter.formatRateInr(item.rate)}/kg"
                                    } else {
                                        "${CurrencyFormatter.formatRateInr(item.rate)}/bag"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Ordered: ${item.effectiveOrderedQty} bags (${item.rateUnitEnum.label})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isPerKgs) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = draft.actualWeightText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,3}$"))) {
                                                itemDrafts[index] = draft.copy(actualWeightText = input)
                                                validationError = null
                                            }
                                        },
                                        label = { Text("Weight (Kgs) *") },
                                        placeholder = { Text("e.g. 248.5") },
                                        suffix = { Text("kg") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dispatch_weight_input_$index"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = draft.actualBagsText,
                                        onValueChange = { input ->
                                            if (input.all { it.isDigit() } && input.length <= 7) {
                                                itemDrafts[index] = draft.copy(actualBagsText = input)
                                                validationError = null
                                            }
                                        },
                                        label = { Text("Actual Bags") },
                                        placeholder = { Text(item.effectiveOrderedQty.toString()) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("dispatch_bags_input_$index"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = draft.actualBagsText,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 7) {
                                            itemDrafts[index] = draft.copy(actualBagsText = input)
                                            validationError = null
                                        }
                                    },
                                    label = { Text("Actual Dispatched Bags *") },
                                    placeholder = { Text("Ordered ${item.effectiveOrderedQty}") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dispatch_bags_input_$index"),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Item Amount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(draft.computedAmount),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Live Final Billing Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = CurrencyFormatter.formatInr(subtotal),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${order.billingTypeEnum.label} (${CurrencyFormatter.formatPercent(order.taxPercent)})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "+ ${CurrencyFormatter.formatInr(taxAmount)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FINAL GRAND TOTAL",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(finalGrandTotal),
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "Dispatch / Completion Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .testTag("dispatch_date_picker_button"),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = DateUtils.formatDisplayDate(dispatchDateIso),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = creditDaysText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            creditDaysText = input
                        }
                    },
                    label = { Text("Payment Credit Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("credit_days_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 6, 7, 15, 30).forEach { days ->
                        FilterChip(
                            selected = creditDaysText == days.toString(),
                            onClick = { creditDaysText = days.toString() },
                            label = { Text("${days}d") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAYMENT DUE DATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = DateUtils.formatDisplayDate(paymentDueIso),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                validationError?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = PriorityHighRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val invalidIndex = itemDrafts.indexOfFirst { draft ->
                        if (draft.sourceItem.rateUnitEnum == RateUnit.PER_KGS) {
                            draft.actualWeightDouble <= 0.0
                        } else {
                            draft.actualBagsInt <= 0
                        }
                    }
                    if (invalidIndex >= 0) {
                        val badDraft = itemDrafts[invalidIndex]
                        val itemName = badDraft.sourceItem.size.ifBlank { "Item #${invalidIndex + 1}" }
                        validationError = if (badDraft.sourceItem.rateUnitEnum == RateUnit.PER_KGS) {
                            "Please enter dispatched Weight (Kgs) for $itemName."
                        } else {
                            "Please enter dispatched Bags count for $itemName."
                        }
                    } else {
                        val updatedItems = itemDrafts.map { it.toDispatchedItem() }
                        onConfirmDispatch(updatedItems, finalGrandTotal, dispatchDateIso, creditDays)
                    }
                },
                modifier = Modifier.testTag("confirm_dispatch_button")
            ) {
                Text("Confirm Dispatch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    order: WorkOrder,
    onDismiss: () -> Unit,
    onConfirmPayment: (
        receivedAmount: Double,
        paymentMode: PaymentMode,
        paymentReceiver: String,
        paymentDateIso: String
    ) -> Unit
) {
    val pending = CurrencyFormatter.roundOff(order.pendingBalance)
    val defaultText = pending.toLong().toString()
    val halfPending = CurrencyFormatter.roundOff(pending / 2.0)
    val halfText = halfPending.toLong().toString()

    var amountText by remember { mutableStateOf(defaultText) }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var selectedReceiver by remember { mutableStateOf("") }
    var receiverDropdownExpanded by remember { mutableStateOf(false) }
    var paymentDateIso by remember { mutableStateOf(DateUtils.todayIso()) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val receiverOptions = PaymentMode.receiverOptionsFor(selectedMode)
    val entered = amountText.trim().toDoubleOrNull() ?: 0.0
    val remainingAfter = CurrencyFormatter.roundOff((pending - entered).coerceAtLeast(0.0))

    if (showPaymentDatePicker) {
        IsoDatePickerDialog(
            initialIso = paymentDateIso,
            onDateSelected = { paymentDateIso = it },
            onDismiss = { showPaymentDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Record Payment", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = order.partyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order Total", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = CurrencyFormatter.formatInr(order.totalAmount),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pending Balance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyFormatter.formatInr(pending),
                                style = MonoMetricStyle,
                                color = PriorityHighRed
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$")) && input.length <= 10) {
                            amountText = input
                            errorText = null
                        }
                    },
                    label = { Text("Received Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = amountText == defaultText,
                        onClick = {
                            amountText = defaultText
                            errorText = null
                        },
                        label = { Text("100% (Full)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("payment_percent_100")
                    )
                    FilterChip(
                        selected = amountText == halfText,
                        onClick = {
                            amountText = halfText
                            errorText = null
                        },
                        label = { Text("50%") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("payment_percent_50")
                    )
                }

                // Payment Mode: Cash or Online
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Received Via",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PaymentMode.entries.forEach { mode ->
                            val isSelected = selectedMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedMode = mode
                                        val validForMode = PaymentMode.receiverOptionsFor(mode)
                                        if (selectedReceiver !in validForMode) {
                                            selectedReceiver = ""
                                        }
                                        receiverDropdownExpanded = false
                                        errorText = null
                                    }
                                    .testTag("payment_mode_${mode.label.lowercase()}"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (mode == PaymentMode.CASH) Icons.Filled.Payments else Icons.Filled.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Receiver Dropdown (for both Cash and Online)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (selectedMode == PaymentMode.CASH) {
                            "Cash Received By *"
                        } else {
                            "Online Payment Receiver *"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExposedDropdownMenuBox(
                        expanded = receiverDropdownExpanded,
                        onExpandedChange = { receiverDropdownExpanded = !receiverDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedReceiver.ifBlank { "Select Receiver" },
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = receiverDropdownExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("payment_receiver_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = receiverDropdownExpanded,
                            onDismissRequest = { receiverDropdownExpanded = false }
                        ) {
                            receiverOptions.forEachIndexed { idx, option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            fontWeight = if (selectedReceiver == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedReceiver = option
                                        receiverDropdownExpanded = false
                                        errorText = null
                                    },
                                    modifier = Modifier.testTag("payment_receiver_option_$idx")
                                )
                            }
                        }
                    }
                }

                // Payment Date
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment Date",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { showPaymentDatePicker = true }
                            .testTag("payment_date_picker_button"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = DateUtils.formatDisplayDate(paymentDateIso),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = PriorityHighRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Remaining after payment: ${CurrencyFormatter.formatInr(remainingAfter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.trim().toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorText = "Enter a valid amount greater than 0."
                    } else if (selectedReceiver.isBlank()) {
                        errorText = if (selectedMode == PaymentMode.CASH) {
                            "Please select who received the cash payment."
                        } else {
                            "Please select who received the online payment."
                        }
                    } else {
                        onConfirmPayment(
                            CurrencyFormatter.roundOff(amt),
                            selectedMode,
                            selectedReceiver,
                            paymentDateIso
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Save Payment")
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
fun SettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaveSettings: (webAppUrl: String, partnerOne: String, partnerTwo: String) -> Unit,
    onCopiedScript: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var urlInput by remember { mutableStateOf(settings.webAppUrl) }
    var partnerOne by remember { mutableStateOf(settings.partnerOneName) }
    var partnerTwo by remember { mutableStateOf(settings.partnerTwoName) }
    var showAppsScriptGuide by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Factory & Sync Settings",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_factory_banner),
                    contentDescription = "JSM Industries Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Google Apps Script Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_webapp_url_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Last synced: ${DateUtils.formatSyncTime(settings.lastSyncedAt)} • Auto-syncs every 30s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = partnerOne,
                        onValueChange = { partnerOne = it },
                        label = { Text("Partner 1") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_partner_one_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = partnerTwo,
                        onValueChange = { partnerTwo = it },
                        label = { Text("Partner 2") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_partner_two_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Expandable Google Apps Script Code.gs helper
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showAppsScriptGuide = !showAppsScriptGuide },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Apps Script Setup Code (Code.gs)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (showAppsScriptGuide) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                        if (showAppsScriptGuide) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Paste into Extensions → Apps Script in your Google Sheet, then Deploy → Web App (Execute as: Me, Who has access: Anyone).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(AppsScriptGuide.CODE_GS))
                                    onCopiedScript()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Code.gs")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveSettings(urlInput, partnerOne, partnerTwo) },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DeleteOrderConfirmDialog(
    order: WorkOrder,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Work Order?") },
        text = {
            Text(
                text = "Remove ${order.partyName} (${CurrencyFormatter.formatInr(order.totalAmount)})? This will also remove it from Google Sheets if synced."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
