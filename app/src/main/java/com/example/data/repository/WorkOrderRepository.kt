package com.example.data.repository

import com.example.data.local.OrderDao
import com.example.data.local.OrderEntityMapper
import com.example.data.model.AppSettings
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMode
import com.example.data.model.SyncStatus
import com.example.data.model.WorkOrder
import com.example.data.preferences.AppPreferences
import com.example.data.remote.GoogleSheetsClient
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WorkOrderRepository(
    private val orderDao: OrderDao,
    private val preferences: AppPreferences,
    private val sheetsClient: GoogleSheetsClient
) {
    private val syncMutex = Mutex()

    val ordersFlow: Flow<List<WorkOrder>> = orderDao.observeVisibleOrders()
        .map { entities -> entities.map { OrderEntityMapper.toDomain(it) } }

    val settingsFlow: Flow<AppSettings> = preferences.settingsFlow

    suspend fun getSettings(): AppSettings = preferences.getSettings()

    suspend fun saveSettings(
        webAppUrl: String,
        partnerOneName: String,
        partnerTwoName: String
    ) {
        preferences.saveSettings(webAppUrl, partnerOneName, partnerTwoName)
    }

    suspend fun addOrUpdateOrder(order: WorkOrder, isEdit: Boolean): SyncStatus {
        val action = if (isEdit) "update" else "add"
        val resolvedBilling = order.billingTypeEnum.label
        val normalizedItems = order.items.map { item ->
            item.copy(billingType = resolvedBilling, taxPercent = order.taxPercent)
        }
        val updatedOrder = order.copy(
            items = normalizedItems,
            billingType = resolvedBilling,
            itemsSummary = WorkOrder.buildItemsSummary(normalizedItems, resolvedBilling, order.taxPercent),
            updatedAt = System.currentTimeMillis()
        )
        orderDao.upsertOrder(OrderEntityMapper.toEntity(updatedOrder, pendingSyncAction = action))
        return pushSingleMutation(action, updatedOrder)
    }

    suspend fun completeOrder(
        order: WorkOrder,
        dispatchedItems: List<OrderItem>,
        finalTotalAmount: Double,
        completionDateIso: String,
        creditDays: Int
    ): SyncStatus {
        val dueDateIso = DateUtils.addDaysIso(completionDateIso, creditDays)
        val resolvedBilling = order.billingTypeEnum.label
        val roundedTotal = CurrencyFormatter.roundOff(finalTotalAmount)
        val normalizedItems = dispatchedItems.map { item ->
            item.copy(billingType = resolvedBilling, taxPercent = order.taxPercent)
        }
        val completedOrder = order.copy(
            items = normalizedItems,
            totalAmount = roundedTotal,
            itemsSummary = WorkOrder.buildItemsSummary(normalizedItems, resolvedBilling, order.taxPercent),
            status = OrderStatus.COMPLETED.value,
            completionDate = completionDateIso,
            creditDays = creditDays,
            paymentDueDate = dueDateIso,
            updatedAt = System.currentTimeMillis()
        )
        orderDao.upsertOrder(OrderEntityMapper.toEntity(completedOrder, pendingSyncAction = "update"))
        return pushSingleMutation("update", completedOrder)
    }

    suspend fun recordPayment(
        order: WorkOrder,
        receivedAmount: Double,
        paymentMode: PaymentMode = PaymentMode.CASH,
        paymentReceiver: String? = null,
        paymentDateIso: String = DateUtils.todayIso()
    ): SyncStatus {
        val roundedReceived = CurrencyFormatter.roundOff(receivedAmount)
        val newPaid = (order.paidAmount + roundedReceived).coerceAtMost(order.totalAmount)
        val cleanReceiver = paymentReceiver?.trim()?.takeIf { it.isNotEmpty() }
        val modeDetail = if (cleanReceiver != null) {
            "${paymentMode.label} ($cleanReceiver)"
        } else {
            paymentMode.label
        }
        val cleanDateIso = paymentDateIso.ifBlank { DateUtils.todayIso() }
        val dateDisplay = DateUtils.formatDisplayDate(cleanDateIso)
        val entry = "${CurrencyFormatter.formatInr(roundedReceived)} • $modeDetail • $dateDisplay"
        val updatedLog = listOfNotNull(
            order.paymentLog?.takeIf { it.isNotBlank() },
            entry
        ).joinToString(" | ")
        val resolvedBilling = order.billingTypeEnum.label
        val normalizedItems = order.items.mapIndexed { idx, item ->
            item.copy(
                billingType = resolvedBilling,
                taxPercent = order.taxPercent,
                paymentMode = if (idx == 0) paymentMode.label else null,
                paymentReceiver = if (idx == 0) cleanReceiver else null,
                paymentDate = if (idx == 0) cleanDateIso else null,
                paymentLog = if (idx == 0) updatedLog else null
            )
        }
        val updatedOrder = order.copy(
            items = normalizedItems,
            paidAmount = newPaid,
            paymentMode = paymentMode.label,
            paymentReceiver = cleanReceiver,
            paymentDate = cleanDateIso,
            paymentLog = updatedLog,
            itemsSummary = WorkOrder.buildItemsSummary(
                items = normalizedItems,
                billingType = resolvedBilling,
                taxPercent = order.taxPercent,
                paymentLog = updatedLog
            ),
            updatedAt = System.currentTimeMillis()
        )
        orderDao.upsertOrder(OrderEntityMapper.toEntity(updatedOrder, pendingSyncAction = "update"))
        return pushSingleMutation("update", updatedOrder)
    }

    suspend fun deleteOrder(order: WorkOrder): SyncStatus {
        val settings = preferences.getSettings()
        if (!settings.isUrlConfigured) {
            orderDao.deleteById(order.id)
            return SyncStatus.NotConfigured
        }
        orderDao.upsertOrder(OrderEntityMapper.toEntity(order, pendingSyncAction = "delete"))
        return pushSingleMutation("delete", order)
    }

    suspend fun syncWithSheets(): SyncStatus = syncMutex.withLock {
        val settings = preferences.getSettings()
        if (!settings.isUrlConfigured) {
            return SyncStatus.NotConfigured
        }

        // 1. Flush any locally queued mutations first
        val pendingQueue = orderDao.getPendingSyncOrders()
        var queueFailures = 0
        var lastErrorMsg: String? = null

        for (entity in pendingQueue) {
            val action = entity.pendingSyncAction ?: continue
            val domain = OrderEntityMapper.toDomain(entity)
            val result = sheetsClient.syncOrder(settings.webAppUrl, action, domain)
            if (result.isSuccess) {
                if (action == "delete") {
                    orderDao.deleteById(entity.id)
                } else {
                    orderDao.upsertOrder(entity.copy(pendingSyncAction = null))
                }
            } else {
                queueFailures++
                lastErrorMsg = result.exceptionOrNull()?.message ?: "Sync failed"
            }
        }

        // 2. Fetch latest snapshot from Google Sheets
        val fetchResult = sheetsClient.fetchOrders(settings.webAppUrl)
        return fetchResult.fold(
            onSuccess = { remoteOrders ->
                val entities = remoteOrders.map { OrderEntityMapper.toEntity(it, pendingSyncAction = null) }
                orderDao.replaceSyncedOrders(entities)
                val now = System.currentTimeMillis()
                preferences.updateLastSyncedAt(now)
                val remainingQueue = orderDao.getPendingSyncCount()
                if (queueFailures > 0 && lastErrorMsg != null) {
                    SyncStatus.Error(lastErrorMsg, remainingQueue)
                } else {
                    SyncStatus.Synced(now, remainingQueue)
                }
            },
            onFailure = { err ->
                val remainingQueue = orderDao.getPendingSyncCount()
                SyncStatus.Error(err.message ?: "Unable to reach Google Sheet", remainingQueue)
            }
        )
    }

    private suspend fun pushSingleMutation(action: String, order: WorkOrder): SyncStatus {
        val settings = preferences.getSettings()
        if (!settings.isUrlConfigured) {
            return SyncStatus.NotConfigured
        }

        val result = sheetsClient.syncOrder(settings.webAppUrl, action, order)
        return result.fold(
            onSuccess = {
                if (action == "delete") {
                    orderDao.deleteById(order.id)
                } else {
                    orderDao.upsertOrder(OrderEntityMapper.toEntity(order, pendingSyncAction = null))
                }
                val now = System.currentTimeMillis()
                preferences.updateLastSyncedAt(now)
                SyncStatus.Synced(now, orderDao.getPendingSyncCount())
            },
            onFailure = { err ->
                SyncStatus.Error(err.message ?: "Saved locally; sync pending", orderDao.getPendingSyncCount())
            }
        )
    }
}
