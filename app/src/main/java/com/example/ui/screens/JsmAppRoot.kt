package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.WorkOrder
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.IndustrialAmberBright
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.SlateNavy900
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsmAppRoot(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var dispatchingOrder by remember { mutableStateOf<WorkOrder?>(null) }
    var payingOrder by remember { mutableStateOf<WorkOrder?>(null) }
    var deletingOrder by remember { mutableStateOf<WorkOrder?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = uiState.activeTab != AppTab.PENDING) {
        if (uiState.activeTab == AppTab.NEW_ORDER && uiState.editingOrder != null) {
            viewModel.cancelEditingOrder()
        }
        viewModel.selectTab(AppTab.PENDING)
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = uiState.settings,
            onDismiss = { showSettingsDialog = false },
            onSaveSettings = { url, p1, p2 ->
                viewModel.saveSettings(url, p1, p2)
                showSettingsDialog = false
            },
            onCopiedScript = {
                scope.launch { snackbarHostState.showSnackbar("Apps Script Code.gs copied to clipboard") }
            }
        )
    }

    dispatchingOrder?.let { order ->
        DispatchOrderDialog(
            order = order,
            onDismiss = { dispatchingOrder = null },
            onConfirmDispatch = { dispatchedItems, finalTotal, dateIso, creditDays ->
                viewModel.completeOrder(order, dispatchedItems, finalTotal, dateIso, creditDays)
                dispatchingOrder = null
            }
        )
    }

    payingOrder?.let { order ->
        RecordPaymentDialog(
            order = order,
            onDismiss = { payingOrder = null },
            onConfirmPayment = { amount, mode, receiver, paymentDateIso ->
                viewModel.recordPayment(order, amount, mode, receiver, paymentDateIso)
                payingOrder = null
            }
        )
    }

    deletingOrder?.let { order ->
        DeleteOrderConfirmDialog(
            order = order,
            onDismiss = { deletingOrder = null },
            onConfirmDelete = {
                viewModel.deleteOrder(order)
                deletingOrder = null
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateNavy900,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(IndustrialAmberBright),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "JSM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SlateNavy900
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.header_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.header_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        SyncStatusPill(
                            syncStatus = uiState.syncStatus,
                            onClick = {
                                if (!uiState.settings.isUrlConfigured) {
                                    showSettingsDialog = true
                                } else {
                                    viewModel.syncNow(isAuto = false)
                                }
                            }
                        )
                        IconButton(
                            onClick = { viewModel.syncNow(isAuto = false) },
                            modifier = Modifier.testTag("manual_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sync now",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        reminderCount = uiState.reminderOrders.size
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = uiState.activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        reminderCount = uiState.reminderOrders.size
                                    )
                                },
                                label = { Text(tab.title) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()

                    when (uiState.activeTab) {
                        AppTab.PENDING -> PendingOrdersScreen(
                            orders = uiState.pendingOrders,
                            reminderOrders = uiState.reminderOrders,
                            searchQuery = uiState.pendingSearchQuery,
                            selectedPriority = uiState.pendingPriorityFilter,
                            onSearchChange = viewModel::updatePendingSearch,
                            onPriorityFilterChange = viewModel::updatePendingPriorityFilter,
                            onOpenReminders = { viewModel.selectTab(AppTab.REMINDERS) },
                            onOpenNewOrder = { viewModel.selectTab(AppTab.NEW_ORDER) },
                            onEditOrder = viewModel::startEditingOrder,
                            onDeleteOrder = { deletingOrder = it },
                            onMarkComplete = { dispatchingOrder = it },
                            modifier = contentModifier
                        )

                        AppTab.NEW_ORDER -> NewOrderScreen(
                            settings = uiState.settings,
                            partySuggestions = uiState.partySuggestions,
                            editingOrder = uiState.editingOrder,
                            onCancelEdit = viewModel::cancelEditingOrder,
                            onSaveOrder = viewModel::saveOrder,
                            modifier = contentModifier
                        )

                        AppTab.COMPLETED -> CompletedOrdersScreen(
                            orders = uiState.completedOrders,
                            totalDispatchedAmount = uiState.totalDispatchedAmount,
                            totalPendingReceivable = uiState.totalPendingReceivable,
                            searchQuery = uiState.completedSearchQuery,
                            selectedFilter = uiState.completedPaymentFilter,
                            onSearchChange = viewModel::updateCompletedSearch,
                            onFilterChange = viewModel::updateCompletedPaymentFilter,
                            onRecordPayment = { payingOrder = it },
                            onDeleteOrder = { deletingOrder = it },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                            modifier = contentModifier
                        )

                        AppTab.REMINDERS -> RemindersScreen(
                            reminders = uiState.reminderOrders,
                            onRecordPayment = { payingOrder = it },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    reminderCount: Int
) {
    val iconVector = when (tab) {
        AppTab.PENDING -> if (selected) Icons.Filled.Assignment else Icons.Outlined.Assignment
        AppTab.NEW_ORDER -> if (selected) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline
        AppTab.COMPLETED -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
        AppTab.REMINDERS -> if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
    }

    if (tab == AppTab.REMINDERS && reminderCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = PriorityHighRed,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (reminderCount > 99) "99+" else reminderCount.toString(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = tab.title
            )
        }
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = tab.title
        )
    }
}
