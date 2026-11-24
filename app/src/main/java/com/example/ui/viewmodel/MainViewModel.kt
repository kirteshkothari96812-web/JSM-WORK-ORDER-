package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Priority
import com.example.data.model.SyncStatus
import com.example.data.model.WorkOrder
import com.example.data.repository.WorkOrderRepository
import com.example.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    PENDING("Pending"),
    NEW_ORDER("New Order"),
    COMPLETED("Completed"),
    REMINDERS("Reminders")
}

enum class PaymentFilter(val label: String) {
    ALL("All"),
    UNPAID("Unpaid"),
    PARTIAL("Partial"),
    PAID("Paid")
}

data class ReminderOrderUi(
    val order: WorkOrder,
    val daysUntilDue: Int
)

data class MainUiState(
    val activeTab: AppTab = AppTab.PENDING,
    val settings: AppSettings = AppSettings(),
    val syncStatus: SyncStatus = SyncStatus.NotConfigured,
    val pendingSearchQuery: String = "",
    val pendingPriorityFilter: Priority? = null,
    val completedSearchQuery: String = "",
    val completedPaymentFilter: PaymentFilter = PaymentFilter.ALL,
    val pendingOrders: List<WorkOrder> = emptyList(),
    val completedOrders: List<WorkOrder> = emptyList(),
    val reminderOrders: List<ReminderOrderUi> = emptyList(),
    val partySuggestions: List<String> = emptyList(),
    val editingOrder: WorkOrder? = null,
    val totalPendingReceivable: Double = 0.0,
    val totalDispatchedAmount: Double = 0.0
)

class MainViewModel(
    private val repository: WorkOrderRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(AppTab.PENDING)
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.NotConfigured)
    private val _pendingSearchQuery = MutableStateFlow("")
    private val _pendingPriorityFilter = MutableStateFlow<Priority?>(null)
    private val _completedSearchQuery = MutableStateFlow("")
    private val _completedPaymentFilter = MutableStateFlow(PaymentFilter.ALL)
    private val _editingOrder = MutableStateFlow<WorkOrder?>(null)

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val filterState = combine(
        _activeTab,
        _syncStatus,
        _pendingSearchQuery,
        _pendingPriorityFilter,
        _completedSearchQuery
    ) { tab, sync, pSearch, pPriority, cSearch ->
        FilterBundle(tab, sync, pSearch, pPriority, cSearch)
    }

    val uiState: StateFlow<MainUiState> = combine(
        repository.ordersFlow,
        repository.settingsFlow,
        filterState,
        _completedPaymentFilter,
        _editingOrder
    ) { allOrders, settings, filters, cPaymentFilter, editing ->
        val todayIso = DateUtils.todayIso()

        val allPending = allOrders
            .filter { it.statusEnum == OrderStatus.PENDING }
            .sortedWith(
                compareBy<WorkOrder> { it.priorityEnum.sortRank }
                    .thenBy { it.expectedDeliveryDate.ifBlank { "9999-12-31" } }
                    .thenByDescending { it.updatedAt }
            )

        val filteredPending = allPending.filter { order ->
            val matchesSearch = filters.pendingSearch.isBlank() ||
                order.partyName.contains(filters.pendingSearch.trim(), ignoreCase = true)
            val matchesPriority = filters.pendingPriority == null ||
                order.priorityEnum == filters.pendingPriority
            matchesSearch && matchesPriority
        }

        val allCompleted = allOrders
            .filter { it.statusEnum == OrderStatus.COMPLETED }
            .sortedWith(
                compareByDescending<WorkOrder> { it.completionDate ?: it.orderDate }
                    .thenByDescending { it.updatedAt }
            )

        val filteredCompleted = allCompleted.filter { order ->
            val matchesSearch = filters.completedSearch.isBlank() ||
                order.partyName.contains(filters.completedSearch.trim(), ignoreCase = true)
            val matchesPayment = when (cPaymentFilter) {
                PaymentFilter.ALL -> true
                PaymentFilter.UNPAID -> order.paymentStatus == PaymentStatus.UNPAID
                PaymentFilter.PARTIAL -> order.paymentStatus == PaymentStatus.PARTIAL
                PaymentFilter.PAID -> order.paymentStatus == PaymentStatus.PAID
            }
            matchesSearch && matchesPayment
        }

        val reminders = allCompleted
            .filter { it.pendingBalance > 0.01 && !it.paymentDueDate.isNullOrBlank() }
            .map { order ->
                val daysUntilDue = DateUtils.daysBetweenIso(todayIso, order.paymentDueDate!!)
                ReminderOrderUi(order = order, daysUntilDue = daysUntilDue)
            }
            .filter { it.daysUntilDue <= 1 }
            .sortedBy { it.daysUntilDue }

        val suggestions = allOrders
            .map { it.partyName.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()

        val resolvedSyncStatus = if (!settings.isUrlConfigured) {
            SyncStatus.NotConfigured
        } else if (filters.syncStatus is SyncStatus.NotConfigured && settings.lastSyncedAt > 0L) {
            SyncStatus.Synced(settings.lastSyncedAt)
        } else {
            filters.syncStatus
        }

        MainUiState(
            activeTab = filters.activeTab,
            settings = settings,
            syncStatus = resolvedSyncStatus,
            pendingSearchQuery = filters.pendingSearch,
            pendingPriorityFilter = filters.pendingPriority,
            completedSearchQuery = filters.completedSearch,
            completedPaymentFilter = cPaymentFilter,
            pendingOrders = filteredPending,
            completedOrders = filteredCompleted,
            reminderOrders = reminders,
            partySuggestions = suggestions,
            editingOrder = editing,
            totalPendingReceivable = allCompleted.sumOf { it.pendingBalance },
            totalDispatchedAmount = allCompleted.sumOf { it.totalAmount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        syncNow(isAuto = true)
        startAutoSyncLoop()
    }

    private fun startAutoSyncLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                syncNow(isAuto = true)
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    fun updatePendingSearch(query: String) {
        _pendingSearchQuery.value = query
    }

    fun updatePendingPriorityFilter(priority: Priority?) {
        _pendingPriorityFilter.value = priority
    }

    fun updateCompletedSearch(query: String) {
        _completedSearchQuery.value = query
    }

    fun updateCompletedPaymentFilter(filter: PaymentFilter) {
        _completedPaymentFilter.value = filter
    }

    fun startEditingOrder(order: WorkOrder) {
        _editingOrder.value = order
        _activeTab.value = AppTab.NEW_ORDER
    }

    fun cancelEditingOrder() {
        _editingOrder.value = null
    }

    fun syncNow(isAuto: Boolean = false) {
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (!settings.isUrlConfigured) {
                _syncStatus.value = SyncStatus.NotConfigured
                if (!isAuto) {
                    _events.emit("Set your Google Apps Script URL in Settings to enable cloud sync.")
                }
                return@launch
            }
            _syncStatus.value = SyncStatus.Syncing
            val result = repository.syncWithSheets()
            _syncStatus.value = result
            if (!isAuto) {
                when (result) {
                    is SyncStatus.Synced -> _events.emit("Synced with Google Sheets")
                    is SyncStatus.Error -> _events.emit("Sync error: ${result.message}")
                    SyncStatus.NotConfigured -> Unit
                    SyncStatus.Syncing -> Unit
                }
            }
        }
    }

    fun saveOrder(order: WorkOrder, isEdit: Boolean) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = repository.addOrUpdateOrder(order, isEdit = isEdit)
            _syncStatus.value = result
            _editingOrder.value = null
            _activeTab.value = AppTab.PENDING
            val msg = when (result) {
                is SyncStatus.Synced -> if (isEdit) "Order updated & synced" else "Work order saved & synced"
                is SyncStatus.NotConfigured -> if (isEdit) "Order updated locally" else "Work order saved locally"
                is SyncStatus.Error -> "Saved locally — sync queued (${result.message})"
                SyncStatus.Syncing -> "Order saved"
            }
            _events.emit(msg)
        }
    }

    fun completeOrder(
        order: WorkOrder,
        dispatchedItems: List<com.example.data.model.OrderItem>,
        finalTotalAmount: Double,
        completionDateIso: String,
        creditDays: Int
    ) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = repository.completeOrder(
                order = order,
                dispatchedItems = dispatchedItems,
                finalTotalAmount = finalTotalAmount,
                completionDateIso = completionDateIso,
                creditDays = creditDays
            )
            _syncStatus.value = result
            val msg = when (result) {
                is SyncStatus.Synced -> "${order.partyName} dispatched & synced"
                is SyncStatus.NotConfigured -> "${order.partyName} marked dispatched"
                is SyncStatus.Error -> "Dispatched locally — sync queued"
                SyncStatus.Syncing -> "Dispatched"
            }
            _events.emit(msg)
        }
    }

    fun recordPayment(
        order: WorkOrder,
        receivedAmount: Double,
        paymentMode: com.example.data.model.PaymentMode = com.example.data.model.PaymentMode.CASH,
        paymentReceiver: String? = null,
        paymentDateIso: String = DateUtils.todayIso()
    ) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = repository.recordPayment(
                order = order,
                receivedAmount = receivedAmount,
                paymentMode = paymentMode,
                paymentReceiver = paymentReceiver,
                paymentDateIso = paymentDateIso
            )
            _syncStatus.value = result
            val msg = when (result) {
                is SyncStatus.Synced -> "Payment recorded & synced"
                is SyncStatus.NotConfigured -> "Payment recorded locally"
                is SyncStatus.Error -> "Payment saved locally — sync queued"
                SyncStatus.Syncing -> "Payment recorded"
            }
            _events.emit(msg)
        }
    }

    fun deleteOrder(order: WorkOrder) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val result = repository.deleteOrder(order)
            _syncStatus.value = result
            _events.emit("Order removed")
        }
    }

    fun saveSettings(webAppUrl: String, partnerOneName: String, partnerTwoName: String) {
        viewModelScope.launch {
            repository.saveSettings(webAppUrl, partnerOneName, partnerTwoName)
            syncNow(isAuto = false)
        }
    }

    private data class FilterBundle(
        val activeTab: AppTab,
        val syncStatus: SyncStatus,
        val pendingSearch: String,
        val pendingPriority: Priority?,
        val completedSearch: String
    )

    class Factory(private val repository: WorkOrderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
