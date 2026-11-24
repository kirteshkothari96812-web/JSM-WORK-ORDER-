package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.local.OrderEntityMapper
import com.example.data.model.AppSettings
import com.example.data.model.OrderItem
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.model.RateUnit
import com.example.data.model.WorkOrder
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.SmartSizeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsmLogicUnitTest {

    @Test
    fun partnerDefaults_areKirteshAndSwapnil() {
        val settings = AppSettings()
        assertEquals("Kirtesh", settings.partnerOneName)
        assertEquals("Swapnil", settings.partnerTwoName)
        assertEquals("Kirtesh", WorkOrder().takenBy)
    }

    @Test
    fun indianCurrencyFormatting_roundsOffStandardAndFormatsCroresCorrectly() {
        assertEquals("₹0", CurrencyFormatter.formatInr(0.0))
        assertEquals("₹850", CurrencyFormatter.formatInr(850.0))
        assertEquals("₹12,500", CurrencyFormatter.formatInr(12500.0))
        assertEquals("₹1,24,500", CurrencyFormatter.formatInr(124500.0))
        assertEquals("₹9,850", CurrencyFormatter.formatInr(9849.6))
        assertEquals("₹9,850", CurrencyFormatter.formatInr(9849.5))
        assertEquals("₹9,849", CurrencyFormatter.formatInr(9849.4))
        assertEquals("₹12,34,568", CurrencyFormatter.formatInr(1234567.50))
        assertEquals(9850.0, CurrencyFormatter.roundOff(9849.6), 0.001)
        assertEquals("₹12.75", CurrencyFormatter.formatRateInr(12.75))
    }

    @Test
    fun dateUtils_calculatesExpectedDatesAndDaysBetween() {
        assertEquals("2026-11-27", DateUtils.addDaysIso("2026-11-24", 3))
        assertEquals("2026-12-01", DateUtils.addDaysIso("2026-11-26", 5))
        assertEquals(-2, DateUtils.daysBetweenIso("2026-11-24", "2026-11-22"))
        assertEquals(0, DateUtils.daysBetweenIso("2026-11-24", "2026-11-24"))
        assertEquals(1, DateUtils.daysBetweenIso("2026-11-24", "2026-11-25"))
    }

    @Test
    fun smartSizeFormatter_attachesCrossDirectlyWithoutSpaces() {
        val prev = TextFieldValue("19", TextRange(2))
        val slash = SmartSizeFormatter.applySmartFormatting(prev, TextFieldValue("19/", TextRange(3)))
        assertEquals("19×", slash.text)
        assertEquals(3, slash.selection.start)

        val space = SmartSizeFormatter.applySmartFormatting(prev, TextFieldValue("19 ", TextRange(3)))
        assertEquals("19×", space.text)
        assertEquals(3, space.selection.start)

        val lowerX = SmartSizeFormatter.applySmartFormatting(prev, TextFieldValue("19x", TextRange(3)))
        assertEquals("19×", lowerX.text)

        val decimalSpace = SmartSizeFormatter.applySmartFormatting(
            TextFieldValue("19.5", TextRange(4)),
            TextFieldValue("19.5 ", TextRange(5))
        )
        assertEquals("19.5×", decimalSpace.text)

        val fullExpr = SmartSizeFormatter.applySmartFormatting(
            TextFieldValue("", TextRange(0)),
            TextFieldValue("19x32.5", TextRange(7))
        )
        assertEquals("19×32.5", fullExpr.text)
    }

    @Test
    fun perBagAndPerKgsRateUnits_calculateCorrectlyAndPersist() {
        val perBagItem = OrderItem(
            size = "19×32",
            printType = "Plain",
            rateUnit = RateUnit.PER_BAG.label,
            quantity = 1000,
            rate = 10.0,
            billingType = "GST",
            taxPercent = 18.0
        )
        assertEquals(10000.0, perBagItem.amount, 0.001)

        val perKgsItem = OrderItem(
            size = "24×40",
            printType = "Single Print",
            rateUnit = RateUnit.PER_KGS.label,
            quantity = 500,
            weightKg = 120.5,
            rate = 100.0,
            billingType = "GST",
            taxPercent = 18.0
        )
        assertEquals(12050.0, perKgsItem.amount, 0.001)

        val order = WorkOrder(
            partyName = "Apex Traders",
            takenBy = "Swapnil",
            items = listOf(perBagItem, perKgsItem),
            billingType = "GST",
            taxPercent = 18.0,
            totalAmount = 22050.0 * 1.18
        )

        assertEquals(22050.0, order.subtotalAmount, 0.001)
        assertEquals(3969.0, order.appliedTaxAmount, 0.001)
        assertEquals(26019.0, order.totalAmount, 0.001)
        assertEquals("GST • 18%", order.billingBadgeText)
        assertEquals(PaymentStatus.UNPAID, order.paymentStatus)
        assertEquals(Priority.MID, order.priorityEnum)

        val summary = WorkOrder.buildItemsSummary(order.items, "GST", 18.0)
        assertTrue(summary.contains("[GST 18%]"))
        assertTrue(summary.contains("120.5 kg×₹100/kg"))

        val entity = OrderEntityMapper.toEntity(order)
        val restored = OrderEntityMapper.toDomain(entity)
        assertEquals("Swapnil", restored.takenBy)
        assertEquals("GST", restored.billingType)
        assertEquals(18.0, restored.taxPercent, 0.001)
        assertEquals(RateUnit.PER_KGS.label, restored.items[1].rateUnit)
        assertEquals(120.5, restored.items[1].weightKg, 0.001)
    }

    @Test
    fun dispatchRecalculation_handlesVarianceInBagsAndWeightWithGst() {
        // Stage 1: Pending order taken with 1000 ordered bags Per Kgs (weight pending until dispatch)
        val pendingItem = OrderItem(
            size = "19×32",
            printType = "Plain",
            rateUnit = RateUnit.PER_KGS.label,
            orderedQuantity = 1000,
            quantity = 1000,
            weightKg = 0.0,
            rate = 115.0,
            billingType = "GST",
            taxPercent = 18.0
        )
        assertEquals(0.0, pendingItem.amount, 0.001)
        assertEquals(1000, pendingItem.effectiveOrderedQty)

        val pendingSummary = WorkOrder.buildItemsSummary(listOf(pendingItem), "GST", 18.0)
        assertTrue(pendingSummary.contains("1000 bags @ ₹115/kg (wt pending)"))
        assertTrue(pendingSummary.contains("[GST 18%]"))

        // Stage 2: At dispatch, actual production is 1009 bags weighing 248.5 kg
        val dispatchedItem = pendingItem.copy(
            orderedQuantity = pendingItem.effectiveOrderedQty,
            quantity = 1009,
            weightKg = 248.5
        )
        val subtotal = dispatchedItem.amount // 248.5 * 115 = 28577.5
        assertEquals(28577.5, subtotal, 0.001)

        val gstFinalTotal = subtotal * 1.18 // 33721.45
        val completedOrder = WorkOrder(
            partyName = "Shree Ganesh Pack",
            items = listOf(dispatchedItem),
            billingType = "GST",
            taxPercent = 18.0,
            totalAmount = gstFinalTotal,
            status = "completed"
        )
        assertEquals(33721.45, completedOrder.totalAmount, 0.001)
        assertEquals(1000, completedOrder.items.first().orderedQuantity)
        assertEquals(1009, completedOrder.items.first().quantity)

        val completedSummary = WorkOrder.buildItemsSummary(completedOrder.items, "GST", 18.0)
        assertTrue(completedSummary.contains("248.5 kg×₹115/kg, 1009 bags"))

        val entity = OrderEntityMapper.toEntity(completedOrder)
        val restored = OrderEntityMapper.toDomain(entity)
        assertEquals(1000, restored.items.first().orderedQuantity)
        assertEquals(1009, restored.items.first().quantity)
        assertEquals(248.5, restored.items.first().weightKg, 0.001)
    }

    @Test
    fun paymentModeCashAndOnlineReceiversAndDate_persistAndFormatCorrectly() {
        assertEquals(
            listOf("Kirtesh", "Swapnil"),
            PaymentMode.receiverOptionsFor(PaymentMode.CASH)
        )
        assertEquals(
            listOf("Kirtesh", "N account", "Swapnil account", "Firm account"),
            PaymentMode.receiverOptionsFor(PaymentMode.ONLINE)
        )

        val item = OrderItem(
            size = "19×32",
            printType = "Plain",
            rateUnit = RateUnit.PER_BAG.label,
            orderedQuantity = 500,
            quantity = 500,
            rate = 10.0
        )
        val paidOrder = WorkOrder(
            partyName = "Metro Poly",
            items = listOf(item),
            billingType = "GST",
            taxPercent = 18.0,
            totalAmount = 5900.0,
            paidAmount = 5900.0,
            status = "completed",
            paymentMode = PaymentMode.ONLINE.label,
            paymentReceiver = "Swapnil account",
            paymentDate = "2026-11-24",
            paymentLog = "₹2,950 • Cash (Kirtesh) • 22 Nov 2026 | ₹2,950 • Online (Swapnil account) • 24 Nov 2026"
        )

        assertEquals(PaymentStatus.PAID, paidOrder.paymentStatus)
        assertEquals(
            "₹2,950 • Cash (Kirtesh) • 22 Nov 2026 | ₹2,950 • Online (Swapnil account) • 24 Nov 2026",
            paidOrder.paymentDisplayText
        )

        val summary = WorkOrder.buildItemsSummary(
            items = paidOrder.items,
            billingType = paidOrder.billingType,
            taxPercent = paidOrder.taxPercent,
            paymentLog = paidOrder.paymentLog
        )
        assertTrue(summary.contains("[GST 18%]"))
        assertTrue(summary.contains("[Pay: ₹2,950 • Cash (Kirtesh) • 22 Nov 2026 | ₹2,950 • Online (Swapnil account) • 24 Nov 2026]"))

        val entity = OrderEntityMapper.toEntity(paidOrder)
        val restored = OrderEntityMapper.toDomain(entity)
        assertEquals("Online", restored.paymentMode)
        assertEquals("Swapnil account", restored.paymentReceiver)
        assertEquals("2026-11-24", restored.paymentDate)
        assertEquals(
            "₹2,950 • Cash (Kirtesh) • 22 Nov 2026 | ₹2,950 • Online (Swapnil account) • 24 Nov 2026",
            restored.paymentLog
        )
    }
}
