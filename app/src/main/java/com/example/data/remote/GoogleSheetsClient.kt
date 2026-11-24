package com.example.data.remote

import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PrintType
import com.example.data.model.Priority
import com.example.data.model.SheetSyncRequest
import com.example.data.model.WorkOrder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GoogleSheetsClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(SheetSyncRequest::class.java)
    private val textPlainMediaType = "text/plain;charset=utf-8".toMediaType()

    suspend fun fetchOrders(webAppUrl: String): Result<List<WorkOrder>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(webAppUrl.trim())
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty().trim()
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }
                if (body.startsWith("<")) {
                    throw IllegalStateException("Web App URL returned HTML instead of JSON. Check Apps Script deployment access ('Anyone').")
                }
                parseOrdersJson(body)
            }
        }
    }

    suspend fun syncOrder(
        webAppUrl: String,
        action: String,
        order: WorkOrder
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payloadJson = requestAdapter.toJson(SheetSyncRequest(action = action, order = order))
            val requestBody = payloadJson.toRequestBody(textPlainMediaType)

            val request = Request.Builder()
                .url(webAppUrl.trim())
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty().trim()
                if (!response.isSuccessful) {
                    throw IllegalStateException("Sync failed (HTTP ${response.code})")
                }
                if (body.startsWith("<")) {
                    throw IllegalStateException("Apps Script returned HTML. Ensure Web App is deployed as 'Anyone'.")
                }
                if (body.startsWith("{")) {
                    val obj = JSONObject(body)
                    val status = obj.optString("status", "")
                    if (status.equals("error", ignoreCase = true)) {
                        throw IllegalStateException(obj.optString("message", "Sheet sync error"))
                    }
                }
            }
        }
    }

    private fun parseOrdersJson(rawBody: String): List<WorkOrder> {
        if (rawBody.isBlank()) return emptyList()
        val array: JSONArray = when {
            rawBody.startsWith("[") -> JSONArray(rawBody)
            rawBody.startsWith("{") -> {
                val root = JSONObject(rawBody)
                if (root.optString("status").equals("error", ignoreCase = true)) {
                    throw IllegalStateException(root.optString("message", "Apps Script error"))
                }
                root.optJSONArray("orders")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("rows")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val orders = mutableListOf<WorkOrder>()
        val summaryTagRegex = Regex("\\[(GST|Non-GST)\\s+([0-9]+(?:\\.[0-9]+)?)%]", RegexOption.IGNORE_CASE)
        val paymentTagRegex = Regex("\\[Pay:\\s*(.+?)]", RegexOption.IGNORE_CASE)

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id").trim().ifEmpty { continue }
            val items = parseItems(obj.opt("items"))
            val summaryText = obj.optString("itemsSummary", "")
            val summaryMatch = summaryTagRegex.find(summaryText)
            val paymentMatch = paymentTagRegex.find(summaryText)

            val resolvedBillingType = obj.optString("billingType", "").takeIf { it.isNotBlank() }
                ?: items.firstNotNullOfOrNull { it.billingType }
                ?: summaryMatch?.groupValues?.getOrNull(1)
                ?: com.example.data.model.BillingType.NON_GST.label

            val resolvedTaxPercent = when {
                obj.has("taxPercent") && !obj.isNull("taxPercent") -> parseDouble(obj.opt("taxPercent"), 0.0)
                items.any { it.taxPercent != null } -> items.firstNotNullOfOrNull { it.taxPercent } ?: 0.0
                summaryMatch != null -> summaryMatch.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            val resolvedPaymentMode = obj.optString("paymentMode", "").takeIf { it.isNotBlank() }
                ?: items.firstNotNullOfOrNull { it.paymentMode }
            val resolvedPaymentReceiver = obj.optString("paymentReceiver", "").takeIf { it.isNotBlank() }
                ?: items.firstNotNullOfOrNull { it.paymentReceiver }
            val resolvedPaymentDate = normalizeNullableIsoDate(
                obj.optString("paymentDate", "").takeIf { it.isNotBlank() }
                    ?: items.firstNotNullOfOrNull { it.paymentDate }.orEmpty()
            )
            val resolvedPaymentLog = obj.optString("paymentLog", "").takeIf { it.isNotBlank() }
                ?: items.firstNotNullOfOrNull { it.paymentLog }
                ?: paymentMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }

            val subtotal = items.sumOf { it.amount }
            val computedTotal = com.example.util.CurrencyFormatter.roundOff(
                subtotal + (subtotal * (resolvedTaxPercent / 100.0))
            )
            val rawTotal = parseDouble(obj.opt("totalAmount"), computedTotal)

            orders.add(
                WorkOrder(
                    id = id,
                    partyName = obj.optString("partyName", "").trim(),
                    orderDate = normalizeIsoDate(obj.optString("orderDate", "")),
                    deliveryDays = parseInt(obj.opt("deliveryDays"), 3),
                    expectedDeliveryDate = normalizeIsoDate(obj.optString("expectedDeliveryDate", "")),
                    priority = Priority.fromString(obj.optString("priority", "Mid")).label,
                    takenBy = obj.optString("takenBy", "Kirtesh").ifBlank { "Kirtesh" },
                    items = items,
                    totalAmount = com.example.util.CurrencyFormatter.roundOff(
                        if (rawTotal > 0.0) rawTotal else computedTotal
                    ),
                    status = OrderStatus.fromString(obj.optString("status", "pending")).value,
                    completionDate = normalizeNullableIsoDate(obj.optString("completionDate", "")),
                    creditDays = parseInt(obj.opt("creditDays"), 5),
                    paymentDueDate = normalizeNullableIsoDate(obj.optString("paymentDueDate", "")),
                    paidAmount = parseDouble(obj.opt("paidAmount"), 0.0),
                    updatedAt = parseLong(obj.opt("updatedAt"), System.currentTimeMillis()),
                    billingType = com.example.data.model.BillingType.fromString(resolvedBillingType).label,
                    taxPercent = resolvedTaxPercent,
                    itemsSummary = summaryText.ifBlank {
                        WorkOrder.buildItemsSummary(items, resolvedBillingType, resolvedTaxPercent, resolvedPaymentLog)
                    },
                    paymentMode = resolvedPaymentMode,
                    paymentReceiver = resolvedPaymentReceiver,
                    paymentDate = resolvedPaymentDate,
                    paymentLog = resolvedPaymentLog
                )
            )
        }
        return orders
    }

    private fun parseItems(rawItems: Any?): List<OrderItem> {
        val arr: JSONArray = when (rawItems) {
            is JSONArray -> rawItems
            is String -> {
                val trimmed = rawItems.trim()
                if (trimmed.startsWith("[")) runCatching { JSONArray(trimmed) }.getOrNull() ?: JSONArray()
                else JSONArray()
            }
            else -> JSONArray()
        }

        val list = mutableListOf<OrderItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val itemBillingType = obj.optString("billingType", "").takeIf { it.isNotBlank() }
            val itemTaxPercent = if (obj.has("taxPercent") && !obj.isNull("taxPercent")) {
                parseDouble(obj.opt("taxPercent"), 0.0)
            } else {
                null
            }
            val parsedQty = parseInt(obj.opt("quantity"), 0)
            val parsedOrderedQty = parseInt(obj.opt("orderedQuantity"), parsedQty)
            list.add(
                OrderItem(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    size = obj.optString("size", "").trim(),
                    printType = PrintType.fromString(obj.optString("printType", "Plain")).label,
                    rateUnit = com.example.data.model.RateUnit.fromString(obj.optString("rateUnit", "Per Bag")).label,
                    orderedQuantity = parsedOrderedQty,
                    quantity = parsedQty,
                    weightKg = parseDouble(obj.opt("weightKg"), 0.0),
                    rate = parseDouble(obj.opt("rate"), 0.0),
                    billingType = itemBillingType,
                    taxPercent = itemTaxPercent,
                    paymentMode = obj.optString("paymentMode", "").takeIf { it.isNotBlank() },
                    paymentReceiver = obj.optString("paymentReceiver", "").takeIf { it.isNotBlank() },
                    paymentDate = normalizeNullableIsoDate(obj.optString("paymentDate", "")),
                    paymentLog = obj.optString("paymentLog", "").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }

    private fun normalizeIsoDate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "null") return ""
        return if (trimmed.length >= 10 && trimmed[4] == '-' && trimmed[7] == '-') {
            trimmed.substring(0, 10)
        } else {
            trimmed
        }
    }

    private fun normalizeNullableIsoDate(raw: String): String? {
        val normalized = normalizeIsoDate(raw)
        return normalized.ifBlank { null }
    }

    private fun parseInt(value: Any?, fallback: Int): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.trim().toDoubleOrNull()?.toInt() ?: fallback
            else -> fallback
        }
    }

    private fun parseDouble(value: Any?, fallback: Double): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: fallback
            else -> fallback
        }
    }

    private fun parseLong(value: Any?, fallback: Long): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull() ?: fallback
            else -> fallback
        }
    }
}
