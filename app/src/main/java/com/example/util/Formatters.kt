package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    fun roundOff(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        return BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).toDouble()
    }

    fun formatInr(
        amount: Double,
        includeSymbol: Boolean = true,
        allowDecimals: Boolean = false
    ): String {
        val target = if (allowDecimals) amount else roundOff(amount)
        val negative = target < 0
        val absAmount = abs(target)
        val roundedCents = if (allowDecimals) {
            (absAmount * 100.0).roundToLong()
        } else {
            roundOff(absAmount).toLong() * 100L
        }
        val integerPart = roundedCents / 100
        val decimalPart = (roundedCents % 100).toInt()

        val formattedInt = formatIndianGrouping(integerPart)
        val formattedNumber = if (!allowDecimals || decimalPart == 0) {
            formattedInt
        } else {
            val decStr = decimalPart.toString().padStart(2, '0').trimEnd('0')
            "$formattedInt.$decStr"
        }

        val prefix = buildString {
            if (negative) append("-")
            if (includeSymbol) append("₹")
        }
        return "$prefix$formattedNumber"
    }

    fun formatRateInr(rate: Double, includeSymbol: Boolean = true): String {
        return formatInr(amount = rate, includeSymbol = includeSymbol, allowDecimals = true)
    }

    private fun formatIndianGrouping(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val lastThree = s.substring(s.length - 3)
        val rest = s.substring(0, s.length - 3)
        val groupedRest = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            groupedRest.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                groupedRest.append(',')
            }
        }
        return "${groupedRest.reverse()},$lastThree"
    }

    fun formatPercent(value: Double): String {
        val rounded = (value * 100.0).roundToLong() / 100.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()}%"
        } else {
            val formatted = rounded.toString().trimEnd('0').trimEnd('.')
            "$formatted%"
        }
    }
    fun formatWeight(weight: Double): String {
        val rounded = (weight * 100.0).roundToLong() / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }
}

object DateUtils {
    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    private val displayFormat: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    private val shortFormat: SimpleDateFormat
        get() = SimpleDateFormat("dd MMM", Locale.ENGLISH)

    private val timeFormat: SimpleDateFormat
        get() = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    fun todayIso(): String = isoFormat.format(Calendar.getInstance().time)

    fun millisToIso(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return isoFormat.format(cal.time)
    }

    fun isoToMillis(iso: String): Long? {
        return try {
            isoFormat.parse(iso.trim())?.time
        } catch (_: Exception) {
            null
        }
    }

    fun addDaysIso(startIso: String, days: Int): String {
        val cal = Calendar.getInstance()
        val parsed = try {
            isoFormat.parse(startIso.trim())
        } catch (_: Exception) {
            null
        }
        if (parsed != null) {
            cal.time = parsed
        }
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormat.format(cal.time)
    }

    /**
     * Returns (toIso - fromIso) in whole calendar days.
     * Positive means toIso is in the future relative to fromIso.
     * Negative means toIso is in the past (overdue).
     */
    fun daysBetweenIso(fromIso: String, toIso: String): Int {
        val fromCal = parseZeroedCalendar(fromIso) ?: return 0
        val toCal = parseZeroedCalendar(toIso) ?: return 0
        val diffMillis = toCal.timeInMillis - fromCal.timeInMillis
        return (diffMillis / 86_400_000L).toInt()
    }

    fun formatDisplayDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val parsed = try {
            isoFormat.parse(iso.trim())
        } catch (_: Exception) {
            null
        }
        return if (parsed != null) displayFormat.format(parsed) else iso
    }

    fun formatShortDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val parsed = try {
            isoFormat.parse(iso.trim())
        } catch (_: Exception) {
            null
        }
        return if (parsed != null) shortFormat.format(parsed) else iso
    }

    fun formatSyncTime(millis: Long): String {
        if (millis <= 0L) return "Never"
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return timeFormat.format(cal.time)
    }

    private fun parseZeroedCalendar(iso: String): Calendar? {
        val parsed = try {
            isoFormat.parse(iso.trim())
        } catch (_: Exception) {
            null
        } ?: return null
        return Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}

object SmartSizeFormatter {
    private val separatorTriggerRegex = Regex("^(\\d+(?:\\.\\d+)?)[/xX ]$")

    /**
     * Smart bag size formatter:
     * Typing '/', 'x', 'X', or ' ' immediately after the first dimension (e.g., "19/", "19 ", "19x", "19.5 ")
     * converts it directly into "19×" attached straight to the number with no extra spaces.
     */
    fun applySmartFormatting(previous: TextFieldValue, incoming: TextFieldValue): TextFieldValue {
        val raw = incoming.text

        // If user is backspacing across "×", clean cleanly
        if (raw.length < previous.text.length && previous.text.contains("×") && !raw.contains("×")) {
            val cleaned = raw.trimEnd()
            return TextFieldValue(text = cleaned, selection = TextRange(cleaned.length))
        }

        // Convert trigger character immediately after first numeric token into "×" directly
        val match = separatorTriggerRegex.matchEntire(raw)
        if (match != null) {
            val firstDim = match.groupValues[1]
            val formatted = "${firstDim}×"
            return TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }

        // Handle pasting or fast typing like "19x32.5", "19/32.5", or "19 32.5"
        val fullExprRegex = Regex("^(\\d+(?:\\.\\d+)?)\\s*[/xX× ]\\s*(\\d*(?:\\.\\d*)?)$")
        val fullMatch = fullExprRegex.matchEntire(raw)
        if (fullMatch != null) {
            val w = fullMatch.groupValues[1]
            val h = fullMatch.groupValues[2]
            val formatted = "$w×$h"
            return TextFieldValue(text = formatted, selection = TextRange(formatted.length))
        }

        return incoming
    }
}

object AppsScriptGuide {
    val CODE_GS = """
function doGet() {
  var sheet = getOrCreateSheet_();
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) {
    return jsonResponse_({ orders: [] });
  }
  var headers = data[0];
  var orders = [];
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    if (!row[0]) continue;
    var obj = {};
    for (var j = 0; j < headers.length; j++) {
      obj[headers[j]] = row[j];
    }
    if (typeof obj.items === 'string' && obj.items.length > 0) {
      try { obj.items = JSON.parse(obj.items); } catch (e) { obj.items = []; }
    }
    orders.push(obj);
  }
  return jsonResponse_({ orders: orders });
}

function doPost(e) {
  var payload = JSON.parse(e.postData.contents);
  var action = payload.action;
  var order = payload.order;
  var sheet = getOrCreateSheet_();
  var data = sheet.getDataRange().getValues();
  var rowIndex = -1;
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]) === String(order.id)) {
      rowIndex = i + 1;
      break;
    }
  }
  if (action === 'delete') {
    if (rowIndex > 0) sheet.deleteRow(rowIndex);
    return jsonResponse_({ status: 'ok' });
  }
  var rowValues = [
    order.id, order.partyName, order.orderDate, order.deliveryDays,
    order.expectedDeliveryDate, order.priority, order.takenBy,
    JSON.stringify(order.items || []), order.totalAmount, order.status,
    order.completionDate || '', order.creditDays || 5,
    order.paymentDueDate || '', order.paidAmount || 0, order.updatedAt || Date.now()
  ];
  if (rowIndex > 0) {
    sheet.getRange(rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
  }
  return jsonResponse_({ status: 'ok' });
}

function getOrCreateSheet_() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName('WorkOrders') || ss.insertSheet('WorkOrders');
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
      'id','partyName','orderDate','deliveryDays','expectedDeliveryDate',
      'priority','takenBy','items','totalAmount','status',
      'completionDate','creditDays','paymentDueDate','paidAmount','updatedAt'
    ]);
  }
  return sheet;
}

function jsonResponse_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
""".trimIndent()
}
