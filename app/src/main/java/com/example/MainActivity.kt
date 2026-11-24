package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.OrderStatus
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.KedaiKuTopBar
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StockAlertBanner
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ReportsBackupScreen
import com.example.ui.theme.KedaiKuTheme
import com.example.util.LowStockNotifier

private enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    POS("Kasir", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    ORDERS("Pesanan", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    INVENTORY("Stok", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    ANALYTICS("Analitik", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    REPORTS("Laporan", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LowStockNotifier.ensureChannel(this)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            KedaiKuTheme(themeMode = state.themeMode) {
                KedaiKuRoot(viewModel = viewModel, state = state)
            }
        }
    }
}

@Composable
private fun KedaiKuRoot(
    viewModel: MainViewModel,
    state: MainUiState
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.POS) }
    var hasNotifPermission by remember {
        mutableStateOf(LowStockNotifier.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || LowStockNotifier.hasNotificationPermission(context)
        if (granted) {
            viewModel.triggerLowStockNotificationCheck()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.currentUser == null) {
        LoginScreen(
            storeName = state.storeProfile.storeName,
            hasAnyOwner = state.hasAnyOwner,
            activeStaff = state.activeStaff,
            loginError = state.loginError,
            themeMode = state.themeMode,
            onToggleTheme = { viewModel.cycleThemeMode() },
            onSetupOwner = { store, owner, pin, seed ->
                viewModel.setupInitialOwner(store, owner, pin, seed)
            },
            onLoginWithPin = { id, pin ->
                viewModel.loginWithPin(id, pin)
            },
            onClearError = { viewModel.clearLoginError() }
        )
        return
    }

    if (currentTab != AppTab.POS) {
        BackHandler { currentTab = AppTab.POS }
    }

    val activeOrDraftCount = remember(state.orders) {
        state.orders.count {
            it.order.status == OrderStatus.DRAFT.name ||
                it.order.status == OrderStatus.PENDING.name ||
                it.order.status == OrderStatus.PREPARING.name ||
                it.order.status == OrderStatus.READY.name
        }
    }
    val lowStockCount = state.lowStockItems.size
    val outOfStockCount = remember(state.lowStockItems) {
        state.lowStockItems.count { it.isOutOfStock }
    }
    val needsNotifPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                KedaiKuTopBar(
                    storeName = state.storeProfile.storeName,
                    currentUser = state.currentUser,
                    lowStockCount = lowStockCount,
                    lastAutoBackupTime = state.lastAutoBackupTime,
                    themeMode = state.themeMode,
                    onToggleTheme = { viewModel.cycleThemeMode() },
                    onLowStockClick = { currentTab = AppTab.INVENTORY },
                    onLogoutClick = { viewModel.logout() }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        activeOrderCount = activeOrDraftCount,
                                        lowStockCount = lowStockCount
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
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
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        activeOrderCount = activeOrDraftCount,
                                        lowStockCount = lowStockCount
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (currentTab == AppTab.POS || currentTab == AppTab.INVENTORY) {
                        StockAlertBanner(
                            lowStockCount = lowStockCount,
                            outOfStockCount = outOfStockCount,
                            needsNotificationPermission = needsNotifPermission,
                            onRequestNotificationPermission = requestNotifications,
                            onInspectInventory = { currentTab = AppTab.INVENTORY }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            AppTab.POS -> PosScreen(
                                menuItems = state.availableMenu,
                                categories = state.categories,
                                customizationOptions = state.customizationOptions,
                                cartLines = state.cartLines,
                                draftEditContext = state.draftEditContext,
                                storeProfile = state.storeProfile,
                                isOwner = state.isOwner,
                                themeMode = state.themeMode,
                                onAddCustomizedLine = { item, qty, unitPrice, summary ->
                                    viewModel.addCustomizedCartLine(item, qty, unitPrice, summary)
                                },
                                onUpdateCartLineQty = { lineKey, delta ->
                                    viewModel.updateCartLineQuantity(lineKey, delta)
                                },
                                onClearCart = { viewModel.clearCart() },
                                onCancelDraftEdit = { viewModel.cancelDraftEditing() },
                                onSaveCustomizationOption = { option ->
                                    viewModel.saveCustomizationOption(option)
                                },
                                onDeleteCustomizationOption = { option ->
                                    viewModel.deleteCustomizationOption(option)
                                },
                                onSubmitOrder = { customer, type, pay, discount, paid, notes, saveAsDraft ->
                                    viewModel.placeOrSaveOrder(
                                        customerName = customer,
                                        orderType = type,
                                        paymentMethod = pay,
                                        discountAmount = discount,
                                        paidAmount = paid,
                                        notes = notes,
                                        saveAsDraft = saveAsDraft
                                    )
                                }
                            )

                            AppTab.ORDERS -> OrdersScreen(
                                orders = state.orders,
                                themeMode = state.themeMode,
                                onEditDraft = { draftOrder ->
                                    viewModel.loadDraftOrderIntoCart(draftOrder)
                                    currentTab = AppTab.POS
                                },
                                onDeleteDraft = { orderId -> viewModel.deleteDraftOrder(orderId) },
                                onUpdateStatus = { owi, status -> viewModel.updateOrderStatus(owi, status) },
                                onOpenReceipt = { owi -> viewModel.openReceipt(owi) }
                            )

                            AppTab.INVENTORY -> InventoryScreen(
                                inventoryWithCategories = state.inventoryWithCategories,
                                categories = state.categories,
                                customizationOptions = state.customizationOptions,
                                movements = state.recentMovements,
                                isOwner = state.isOwner,
                                themeMode = state.themeMode,
                                onSaveItem = { item, catIds -> viewModel.saveInventoryItem(item, catIds) },
                                onAdjustStock = { item, delta, reason -> viewModel.adjustStock(item, delta, reason) },
                                onDeleteItem = { item -> viewModel.deleteInventoryItem(item) },
                                onSaveCategory = { id, name, desc -> viewModel.saveCategory(id, name, desc) },
                                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                                onSaveCustomizationOption = { opt -> viewModel.saveCustomizationOption(opt) },
                                onDeleteCustomizationOption = { opt -> viewModel.deleteCustomizationOption(opt) },
                                onCheckLowStockNotifications = {
                                    if (needsNotifPermission) {
                                        requestNotifications()
                                    } else {
                                        viewModel.triggerLowStockNotificationCheck()
                                    }
                                }
                            )

                            AppTab.ANALYTICS -> AnalyticsScreen(
                                period = state.analyticsPeriod,
                                analytics = state.analytics,
                                expenses = state.expenses,
                                isOwner = state.isOwner,
                                themeMode = state.themeMode,
                                onSelectPeriod = { viewModel.setAnalyticsPeriod(it) },
                                onSaveExpense = { id, title, cat, amount, notes, date ->
                                    viewModel.saveExpense(id, title, cat, amount, notes, date)
                                },
                                onDeleteExpense = { viewModel.deleteExpense(it) }
                            )

                            AppTab.REPORTS -> ReportsBackupScreen(
                                isOwner = state.isOwner,
                                storeProfile = state.storeProfile,
                                themeMode = state.themeMode,
                                autoBackupEnabled = state.autoBackupEnabled,
                                lastAutoBackupTime = state.lastAutoBackupTime,
                                localSnapshots = state.localSnapshots,
                                allStaff = state.allStaff,
                                currentUser = state.currentUser,
                                customizationOptions = state.customizationOptions,
                                expenses = state.expenses,
                                needsNotificationPermission = needsNotifPermission,
                                onSetThemeMode = { viewModel.setThemeMode(it) },
                                onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                                onRunManualBackup = { viewModel.runManualBackupNow() },
                                onExportExcel = { viewModel.exportExcel(it) },
                                onExportCsv = { viewModel.exportCsv(it) },
                                onExportPdf = { viewModel.exportPdf(it) },
                                onExportTemplateCsv = { viewModel.exportTemplateCsv(it) },
                                onImportInventoryCsv = { viewModel.importInventoryCsv(it) },
                                onExportBackupJson = { viewModel.exportBackupToUri(it) },
                                onRestoreBackupJson = { viewModel.restoreBackupFromUri(it) },
                                onRestoreLocalSnapshot = { viewModel.restoreLocalSnapshot(it) },
                                onUpdateStoreProfile = { viewModel.updateStoreProfile(it) },
                                onSaveCustomizationOption = { viewModel.saveCustomizationOption(it) },
                                onDeleteCustomizationOption = { viewModel.deleteCustomizationOption(it) },
                                onSaveExpense = { id, title, cat, amount, notes, date ->
                                    viewModel.saveExpense(id, title, cat, amount, notes, date)
                                },
                                onDeleteExpense = { viewModel.deleteExpense(it) },
                                onAddStaff = { name, role, pin -> viewModel.addStaffAccount(name, role, pin) },
                                onToggleStaffActive = { viewModel.toggleStaffActive(it) },
                                onRequestNotificationPermission = requestNotifications,
                                onTestLowStockNotification = {
                                    if (needsNotifPermission) {
                                        requestNotifications()
                                    } else {
                                        viewModel.triggerLowStockNotificationCheck()
                                    }
                                },
                                onLogout = { viewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }

    state.latestReceiptOrder?.let { receiptOrder ->
        ReceiptPreviewDialog(
            orderWithItems = receiptOrder,
            storeProfile = state.storeProfile,
            onDismiss = { viewModel.dismissReceipt() }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    activeOrderCount: Int,
    lowStockCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    val badgeCount = when (tab) {
        AppTab.ORDERS -> activeOrderCount
        AppTab.INVENTORY -> lowStockCount
        else -> 0
    }
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = if (tab == AppTab.INVENTORY) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(badgeCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = tab.label)
        }
    } else {
        Icon(icon, contentDescription = tab.label)
    }
}
