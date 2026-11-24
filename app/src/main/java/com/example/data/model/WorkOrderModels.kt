package com.example.data.model

import com.example.util.CurrencyFormatter
import com.squareup.moshi.JsonClass
import java.util.UUID

enum class Priority(val label: String, val sortRank: Int) {
    HIGH("High", 0),
    MID("Mid", 1),
    LOW("Low", 2);

    companion object {
        fun fromString(value: String?): Priority {
            return entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) } ?: MID
        }
    }
}

enum class PrintType(val label: String) {
    PLAIN("Plain"),
    SINGLE_PRINT("Single Print"),
    DOUBLE_SIDE_PRINT("Double Side Print");

    companion object {
        fun fromString(value: String?): PrintType {
            return entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) } ?: PLAIN
        }
    }
}

enum class RateUnit(val label: String) {
    PER_BAG("Per Bag"),
    PER_KGS("Per Kgs");

    companion object {
        fun fromString(value: String?): RateUnit {
            val normalized = value?.trim().orEmpty()
            return entries.firstOrNull { it.label.equals(normalized, ignoreCase = true) } ?: PER_BAG
        }
    }
}

enum class BillingType(val label: String) {
    GST("GST"),
    NON_GST("Non-GST");

    companion object {
        fun fromString(value: String?): BillingType {
            val normalized = value?.trim().orEmpty()
            return entries.firstOrNull { it.label.equals(normalized, ignoreCase = true) } ?: NON_GST
        }
    }
}

enum class OrderStatus(val value: String) {
    PENDING("pending"),
    COMPLETED("completed");

    companion object {
        fun fromString(value: String?): OrderStatus {
            return entries.firstOrNull { it.value.equals(value?.trim(), ignoreCase = true) } ?: PENDING
        }
    }
}

enum class PaymentStatus(val label: String) {
    UNPAID("Unpaid"),
    PARTIAL("Partial"),
    PAID("Paid")
}

enum class PaymentMode(val label: String) {
    CASH("Cash"),
    ONLINE("Online");

    companion object {
        val CASH_RECEIVER_OPTIONS = listOf(
            "Kirtesh",
            "Swapnil"
        )

        val ONLINE_RECEIVER_OPTIONS = listOf(
            "Kirtesh",
            "N account",
            "Swapnil account",
            "Firm account"
        )

        fun receiverOptionsFor(mode: PaymentMode): List<String> {
            return when (mode) {
                CASH -> CASH_RECEIVER_OPTIONS
                ONLINE -> ONLINE_RECEIVER_OPTIONS
            }
        }

        fun fromString(value: String?): PaymentMode {
            val normalized = value?.trim().orEmpty()
            return entries.firstOrNull { it.label.equals(normalized, ignoreCase = true) } ?: CASH
        }
    }
}

@JsonClass(generateAdapter = true)
data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    val size: String = "",
    val printType: String = PrintType.PLAIN.label,
    val rateUnit: String = RateUnit.PER_BAG.label,
    val orderedQuantity: Int = 0,
    val quantity: Int = 0,
    val weightKg: Double = 0.0,
    val rate: Double = 0.0,
    val billingType: String? = null,
    val taxPercent: Double? = null,
    val paymentMode: String? = null,
    val paymentReceiver: String? = null,
    val paymentDate: String? = null,
    val paymentLog: String? = null
) {
    val rateUnitEnum: RateUnit
        get() = RateUnit.fromString(rateUnit)

    val effectiveOrderedQty: Int
        get() = if (orderedQuantity > 0) orderedQuantity else quantity

    val amount: Double
        get() = if (rateUnitEnum == RateUnit.PER_KGS) {
            weightKg * rate
        } else {
            quantity * rate
        }
}

@JsonClass(generateAdapter = true)
data class WorkOrder(
    val id: String = UUID.randomUUID().toString(),
    val partyName: String = "",
    val orderDate: String = "",
    val deliveryDays: Int = 3,
    val expectedDeliveryDate: String = "",
    val priority: String = Priority.MID.label,
    val takenBy: String = "Kirtesh",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = OrderStatus.PENDING.value,
    val completionDate: String? = null,
    val creditDays: Int = 5,
    val paymentDueDate: String? = null,
    val paidAmount: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis(),
    val billingType: String = BillingType.NON_GST.label,
    val taxPercent: Double = 0.0,
    val itemsSummary: String = "",
    val paymentMode: String? = null,
    val paymentReceiver: String? = null,
    val paymentDate: String? = null,
    val paymentLog: String? = null
) {
    val priorityEnum: Priority
        get() = Priority.fromString(priority)

    val statusEnum: OrderStatus
        get() = OrderStatus.fromString(status)

    val billingTypeEnum: BillingType
        get() = BillingType.fromString(billingType)

    val subtotalAmount: Double
        get() = items.sumOf { it.amount }

    val appliedTaxAmount: Double
        get() = subtotalAmount * (taxPercent / 100.0)

    val billingBadgeText: String
        get() = "${billingTypeEnum.label} • ${CurrencyFormatter.formatPercent(taxPercent)}"

    val pendingBalance: Double
        get() = (totalAmount - paidAmount).coerceAtLeast(0.0)

    val paymentStatus: PaymentStatus
        get() = when {
            pendingBalance <= 0.01 && totalAmount > 0.0 -> PaymentStatus.PAID
            paidAmount > 0.01 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }

    val paymentDisplayText: String?
        get() = when {
            !paymentLog.isNullOrBlank() -> paymentLog
            !paymentMode.isNullOrBlank() -> {
                val base = if (!paymentReceiver.isNullOrBlank()) {
                    "$paymentMode ($paymentReceiver)"
                } else {
                    paymentMode
                }
                if (!paymentDate.isNullOrBlank()) {
                    "$base • ${com.example.util.DateUtils.formatDisplayDate(paymentDate)}"
                } else {
                    base
                }
            }
            else -> null
        }

    companion object {
        fun buildItemsSummary(
            items: List<OrderItem>,
            billingType: String,
            taxPercent: Double,
            paymentLog: String? = null
        ): String {
            val itemLines = items.joinToString("; ") { item ->
                val qtyDesc = if (item.rateUnitEnum == RateUnit.PER_KGS) {
                    val bagPart = if (item.quantity > 0) ", ${item.quantity} bags" else ""
                    if (item.weightKg > 0.0) {
                        "${CurrencyFormatter.formatWeight(item.weightKg)} kg×${CurrencyFormatter.formatRateInr(item.rate)}/kg$bagPart"
                    } else {
                        "${item.effectiveOrderedQty} bags @ ${CurrencyFormatter.formatRateInr(item.rate)}/kg (wt pending)"
                    }
                } else {
                    "${item.quantity}×${CurrencyFormatter.formatRateInr(item.rate)}"
                }
                "${item.size} (${item.printType}, $qtyDesc)"
            }
            val pctText = CurrencyFormatter.formatPercent(taxPercent)
            val tag = "[${BillingType.fromString(billingType).label} $pctText]"
            val base = if (itemLines.isBlank()) tag else "$itemLines $tag"
            return if (!paymentLog.isNullOrBlank()) {
                "$base [Pay: $paymentLog]"
            } else {
                base
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class SheetSyncRequest(
    val action: String,
    val order: WorkOrder
)

sealed class SyncStatus {
    data object NotConfigured : SyncStatus()
    data object Syncing : SyncStatus()
    data class Synced(val timestampMillis: Long, val pendingQueueCount: Int = 0) : SyncStatus()
    data class Error(val message: String, val pendingQueueCount: Int = 0) : SyncStatus()
}

data class AppSettings(
    val webAppUrl: String = DEFAULT_WEB_APP_URL,
    val partnerOneName: String = "Kirtesh",
    val partnerTwoName: String = "Swapnil",
    val lastSyncedAt: Long = 0L
) {
    val isUrlConfigured: Boolean
        get() {
            val trimmed = webAppUrl.trim()
            return trimmed.isNotEmpty() &&
                trimmed != DEFAULT_WEB_APP_URL &&
                (trimmed.startsWith("https://") || trimmed.startsWith("http://"))
        }

    companion object {
        const val DEFAULT_WEB_APP_URL = "[PASTE_YOUR_WEB_APP_URL_HERE]"
    }
}
