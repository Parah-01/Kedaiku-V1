package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.InventoryType
import com.example.data.local.KedaiKuDatabase
import com.example.data.local.MenuRecipeEntity
import com.example.data.local.OrderStatus
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.local.StaffAccountEntity
import com.example.data.local.StaffRole
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.StoreProfile
import com.example.data.repository.CheckoutLineInput
import com.example.data.repository.CheckoutRequest
import com.example.data.repository.KedaiKuRepository
import com.example.ui.theme.ThemeMode
import com.example.util.RecipeStockCalculator
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = KedaiKuDatabase.getInstance(application)
    private val prefs = AppPreferences(application)
    private val repository = KedaiKuRepository(application, db, prefs)

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val cartListType = Types.newParameterizedType(List::class.java, CartLineItem::class.java)
    private val cartAdapter = moshi.adapter<List<CartLineItem>>(cartListType)

    private val analyticsPeriodFlow = MutableStateFlow(AnalyticsPeriod.TODAY)
    private val latestReceiptFlow = MutableStateFlow<OrderWithItems?>(null)
    private val loginErrorFlow = MutableStateFlow<String?>(null)
    private val snapshotsFlow = MutableStateFlow(emptyList<com.example.util.LocalBackupInfo>())
    private val draftEditFlow = MutableStateFlow<DraftEditContext?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.prefsFlow,
        repository.allStaffFlow,
        repository.categoriesAndInventoryFlow(),
        repository.ordersAndMovementsFlow(),
        repository.optionsAndExpensesFlow(),
        analyticsPeriodFlow,
        latestReceiptFlow,
        loginErrorFlow,
        snapshotsFlow,
        draftEditFlow
    ) { values ->
        val prefsState = values[0] as com.example.data.prefs.AppPrefsState
        @Suppress("UNCHECKED_CAST")
        val allStaff = values[1] as List<StaffAccountEntity>
        @Suppress("UNCHECKED_CAST")
        val catsAndInv = values[2] as Pair<List<CategoryEntity>, List<InventoryItemWithCategories>>
        @Suppress("UNCHECKED_CAST")
        val ordersMovements = values[3] as Pair<List<OrderWithItems>, List<com.example.data.local.StockMovementEntity>>
        @Suppress("UNCHECKED_CAST")
        val optionsExpensesRecipes = values[4] as Triple<List<CustomizationOptionEntity>, List<ExpenseEntity>, List<MenuRecipeEntity>>
        val period = values[5] as AnalyticsPeriod
        val receiptOrder = values[6] as OrderWithItems?
        val loginError = values[7] as String?
        @Suppress("UNCHECKED_CAST")
        val snapshots = values[8] as List<com.example.util.LocalBackupInfo>
        val draftEdit = values[9] as DraftEditContext?

        val categories = catsAndInv.first
        val allInventory = catsAndInv.second
        val orders = ordersMovements.first
        val movements = ordersMovements.second
        val customizationOptions = optionsExpensesRecipes.first
        val expenses = optionsExpensesRecipes.second
        val menuRecipes = optionsExpensesRecipes.third
        val activeStaff = allStaff.filter { it.isActive }
        val currentUser = activeStaff.find { it.id == prefsState.activeStaffId }
        val hasOwner = allStaff.any { it.role == StaffRole.OWNER.name && it.isActive }
        val availableMenu = allInventory.filter {
            it.item.itemType == InventoryType.MENU.name && it.item.isAvailable
        }

        val itemsById = allInventory.associate { it.item.id to it.item }
        val recipesByMenu = menuRecipes.groupBy { it.menuItemId }
        val effectiveStockMap = allInventory.associate { entry ->
            entry.item.id to RecipeStockCalculator.computeEffectiveStock(entry.item, recipesByMenu, itemsById)
        }

        val lowStock = allInventory.map { it.item }.filter { item ->
            (effectiveStockMap[item.id] ?: item.stockQty) <= item.minThreshold
        }
        val cartLines = parseCartLines(prefsState.draftCartJson)
            .filter { line ->
                availableMenu.any {
                    it.item.id == line.itemId && (effectiveStockMap[it.item.id] ?: it.item.stockQty) > 0.0
                }
            }

        MainUiState(
            isLoading = false,
            themeMode = prefsState.themeMode,
            storeProfile = prefsState.storeProfile,
            autoBackupEnabled = prefsState.autoBackupEnabled,
            lastAutoBackupTime = prefsState.lastAutoBackupTime,
            localSnapshots = snapshots,
            allStaff = allStaff,
            activeStaff = activeStaff,
            currentUser = currentUser,
            isOwner = currentUser?.role == StaffRole.OWNER.name,
            hasAnyOwner = hasOwner,
            categories = categories,
            customizationOptions = customizationOptions,
            expenses = expenses,
            menuRecipes = menuRecipes,
            effectiveStockMap = effectiveStockMap,
            inventoryWithCategories = allInventory,
            availableMenu = availableMenu,
            lowStockItems = lowStock,
            orders = orders,
            recentMovements = movements,
            cartLines = cartLines,
            draftEditContext = draftEdit,
            analyticsPeriod = period,
            analytics = computeAnalyticsSnapshot(orders, allInventory, expenses, period),
            latestReceiptOrder = receiptOrder,
            loginError = loginError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultCustomizationOptions()
        }
        refreshLocalSnapshots()
    }

    private fun KedaiKuRepository.categoriesAndInventoryFlow() =
        combine(allCategoriesFlow, allInventoryWithCategoriesFlow) { cats, inv -> cats to inv }

    private fun KedaiKuRepository.ordersAndMovementsFlow() =
        combine(allOrdersFlow, recentMovementsFlow) { orders, movements -> orders to movements }

    private fun KedaiKuRepository.optionsAndExpensesFlow() =
        combine(allCustomizationOptionsFlow, allExpensesFlow, allRecipesFlow) { opts, exps, recipes ->
            Triple(opts, exps, recipes)
        }

    fun refreshLocalSnapshots() {
        viewModelScope.launch {
            snapshotsFlow.value = repository.getLocalBackupSnapshots()
        }
    }

    fun setupInitialOwner(
        storeName: String,
        ownerName: String,
        pin: String,
        seedStarterMenu: Boolean
    ) {
        if (pin.length < 4) {
            loginErrorFlow.value = "PIN minimal 4 digit angka."
            return
        }
        viewModelScope.launch {
            loginErrorFlow.value = null
            repository.setupInitialOwner(storeName, ownerName, pin, seedStarterMenu)
            refreshLocalSnapshots()
            _messages.tryEmit("Selamat datang! Toko siap beroperasi offline.")
        }
    }

    fun loginWithPin(staffId: Long, pin: String) {
        viewModelScope.launch {
            val staff = repository.verifyPinAndLogin(staffId, pin)
            if (staff == null) {
                loginErrorFlow.value = "PIN tidak sesuai. Silakan coba lagi."
            } else {
                loginErrorFlow.value = null
                _messages.tryEmit("Masuk sebagai ${staff.name} (${staff.role}).")
            }
        }
    }

    fun clearLoginError() {
        loginErrorFlow.value = null
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            loginErrorFlow.value = null
        }
    }

    fun addStaffAccount(name: String, role: StaffRole, pin: String) {
        if (name.isBlank() || pin.length < 4) {
            _messages.tryEmit("Nama wajib diisi dan PIN minimal 4 digit.")
            return
        }
        viewModelScope.launch {
            repository.addStaffAccount(name, role, pin)
            _messages.tryEmit("Akun ${role.label} '$name' ditambahkan.")
        }
    }

    fun toggleStaffActive(staff: StaffAccountEntity) {
        viewModelScope.launch {
            if (staff.id == uiState.value.currentUser?.id) {
                _messages.tryEmit("Tidak bisa menonaktifkan akun yang sedang dipakai.")
                return@launch
            }
            repository.toggleStaffActive(staff)
        }
    }

    // Category CRUD (Owner)
    fun saveCategory(id: Long, name: String, description: String) {
        if (name.isBlank()) {
            _messages.tryEmit("Nama kategori tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            repository.saveCategory(id, name, description)
            refreshLocalSnapshots()
            _messages.tryEmit(if (id > 0) "Kategori '$name' diperbarui." else "Kategori '$name' dibuat.")
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            refreshLocalSnapshots()
            _messages.tryEmit("Kategori '${category.name}' dihapus.")
        }
    }

    // Customization Option Price CRUD (Owner)
    fun saveCustomizationOption(option: CustomizationOptionEntity) {
        if (option.label.isBlank()) {
            _messages.tryEmit("Nama varian/opsi tidak boleh kosong.")
            return
        }
        viewModelScope.launch {
            repository.saveCustomizationOption(
                option.copy(
                    label = option.label.trim(),
                    extraPrice = option.extraPrice.coerceAtLeast(0L)
                )
            )
            refreshLocalSnapshots()
            _messages.tryEmit(
                if (option.id > 0) "Harga '${option.label.trim()}' diperbarui."
                else "Opsi '${option.label.trim()}' ditambahkan."
            )
        }
    }

    fun deleteCustomizationOption(option: CustomizationOptionEntity) {
        viewModelScope.launch {
            repository.deleteCustomizationOption(option)
            refreshLocalSnapshots()
            _messages.tryEmit("Opsi '${option.label}' dihapus.")
        }
    }

    // Expense Management CRUD (Owner)
    fun saveExpense(
        id: Long,
        title: String,
        category: ExpenseCategory,
        amount: Long,
        notes: String,
        expenseDate: Long = System.currentTimeMillis()
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank() || amount <= 0L) {
            _messages.tryEmit("Judul dan nominal pengeluaran (> 0) wajib diisi.")
            return
        }
        val actor = uiState.value.currentUser?.name ?: "Owner"
        viewModelScope.launch {
            repository.saveExpense(
                ExpenseEntity(
                    id = id,
                    title = cleanTitle,
                    category = category.name,
                    amount = amount,
                    notes = notes.trim(),
                    recordedBy = actor,
                    expenseDate = expenseDate
                )
            )
            refreshLocalSnapshots()
            _messages.tryEmit(
                if (id > 0) "Pengeluaran '$cleanTitle' diperbarui."
                else "Pengeluaran '$cleanTitle' dicatat."
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshLocalSnapshots()
            _messages.tryEmit("Pengeluaran '${expense.title}' dihapus.")
        }
    }

    // Cart management supporting custom variants, toppings, spice levels & recipe stock limits
    fun addCustomizedCartLine(
        item: InventoryItemEntity,
        quantity: Int,
        unitPrice: Long,
        optionsSummary: String
    ) {
        viewModelScope.launch {
            val state = uiState.value
            val cleanSummary = optionsSummary.trim()
            val lineKey = "${item.id}|$cleanSummary"
            val currentLines = state.cartLines.toMutableList()
            val itemsById = state.inventoryWithCategories.associate { it.item.id to it.item }
            val recipesByMenu = state.menuRecipes.groupBy { it.menuItemId }
            val allowedToAdd = RecipeStockCalculator.computeRemainingAddableQty(
                targetMenuId = item.id,
                cartLines = currentLines,
                itemsById = itemsById,
                recipesByMenuId = recipesByMenu
            )

            if (allowedToAdd <= 0) {
                val eff = state.effectiveStockMap[item.id]?.toInt() ?: 0
                _messages.tryEmit("Stok ${item.name} / bahan baku tersisa $eff ${item.unit}.")
                return@launch
            }
            val actualAdd = quantity.coerceIn(1, allowedToAdd)
            val existingIdx = currentLines.indexOfFirst { it.lineKey == lineKey }
            if (existingIdx >= 0) {
                val existing = currentLines[existingIdx]
                currentLines[existingIdx] = existing.copy(
                    quantity = existing.quantity + actualAdd,
                    unitPrice = unitPrice
                )
            } else {
                currentLines.add(
                    CartLineItem(
                        lineKey = lineKey,
                        itemId = item.id,
                        itemName = item.name,
                        basePrice = item.price,
                        unitPrice = unitPrice,
                        quantity = actualAdd,
                        optionsSummary = cleanSummary
                    )
                )
            }
            saveCartLines(currentLines)
        }
    }

    fun updateCartLineQuantity(lineKey: String, delta: Int) {
        viewModelScope.launch {
            val state = uiState.value
            val currentLines = state.cartLines.toMutableList()
            val idx = currentLines.indexOfFirst { it.lineKey == lineKey }
            if (idx < 0) return@launch

            val target = currentLines[idx]
            val itemsById = state.inventoryWithCategories.associate { it.item.id to it.item }
            val recipesByMenu = state.menuRecipes.groupBy { it.menuItemId }
            val allowedExtra = if (delta > 0) {
                RecipeStockCalculator.computeRemainingAddableQty(
                    targetMenuId = target.itemId,
                    cartLines = currentLines,
                    itemsById = itemsById,
                    recipesByMenuId = recipesByMenu
                )
            } else {
                0
            }

            if (delta > 0 && allowedExtra <= 0) {
                val eff = state.effectiveStockMap[target.itemId]?.toInt() ?: 0
                _messages.tryEmit("Stok ${target.itemName} / bahan baku tersisa $eff.")
                return@launch
            }

            val nextQty = if (delta > 0) {
                target.quantity + delta.coerceAtMost(allowedExtra)
            } else {
                (target.quantity + delta).coerceAtLeast(0)
            }

            if (nextQty <= 0) {
                currentLines.removeAt(idx)
            } else {
                currentLines[idx] = target.copy(quantity = nextQty)
            }
            saveCartLines(currentLines)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            draftEditFlow.value = null
            saveCartLines(emptyList())
        }
    }

    fun loadDraftOrderIntoCart(orderWithItems: OrderWithItems) {
        viewModelScope.launch {
            val order = orderWithItems.order
            val lines = orderWithItems.items.map { line ->
                val menuItem = uiState.value.availableMenu.find { it.item.id == line.inventoryItemId }?.item
                val cleanOpts = line.optionsSummary.trim()
                CartLineItem(
                    lineKey = "${line.inventoryItemId}|$cleanOpts",
                    itemId = line.inventoryItemId,
                    itemName = line.itemName,
                    basePrice = menuItem?.price ?: line.unitPrice,
                    unitPrice = line.unitPrice,
                    quantity = line.quantity,
                    optionsSummary = cleanOpts
                )
            }
            draftEditFlow.value = DraftEditContext(
                orderId = order.id,
                orderNumber = order.orderNumber,
                customerName = order.customerName,
                orderType = OrderType.entries.find { it.name == order.orderType } ?: OrderType.TAKE_AWAY,
                paymentMethod = PaymentMethod.entries.find { it.name == order.paymentMethod } ?: PaymentMethod.TUNAI,
                discountAmount = order.discountAmount,
                notes = order.notes
            )
            saveCartLines(lines)
            _messages.tryEmit("Draf ${order.orderNumber} dimuat ke Kasir untuk diedit.")
        }
    }

    fun cancelDraftEditing() {
        viewModelScope.launch {
            draftEditFlow.value = null
            saveCartLines(emptyList())
        }
    }

    fun placeOrSaveOrder(
        customerName: String,
        orderType: OrderType,
        paymentMethod: PaymentMethod,
        discountAmount: Long,
        paidAmount: Long,
        notes: String,
        saveAsDraft: Boolean
    ) {
        val state = uiState.value
        val cashier = state.currentUser ?: return
        val checkoutLines = state.cartLines.mapNotNull { line ->
            val item = state.availableMenu.find { it.item.id == line.itemId }?.item
            if (item != null && line.quantity > 0) {
                CheckoutLineInput(
                    item = item,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    optionsSummary = line.optionsSummary
                )
            } else {
                null
            }
        }
        if (checkoutLines.isEmpty()) {
            _messages.tryEmit("Keranjang masih kosong.")
            return
        }

        viewModelScope.launch {
            try {
                val result = repository.placeOrSaveOrder(
                    CheckoutRequest(
                        existingDraftId = state.draftEditContext?.orderId,
                        cartLines = checkoutLines,
                        customerName = customerName,
                        orderType = orderType,
                        paymentMethod = paymentMethod,
                        discountAmount = discountAmount,
                        paidAmount = paidAmount,
                        notes = notes,
                        cashier = cashier,
                        saveAsDraft = saveAsDraft
                    )
                )
                draftEditFlow.value = null
                refreshLocalSnapshots()
                if (saveAsDraft) {
                    _messages.tryEmit("Draf ${result.order.orderNumber} tersimpan & bisa diedit kapan saja.")
                } else {
                    latestReceiptFlow.value = result
                    _messages.tryEmit("Pesanan ${result.order.orderNumber} selesai diproses.")
                }
            } catch (e: Exception) {
                _messages.tryEmit("Gagal memproses pesanan: ${e.message}")
            }
        }
    }

    fun deleteDraftOrder(orderId: Long) {
        viewModelScope.launch {
            if (draftEditFlow.value?.orderId == orderId) {
                draftEditFlow.value = null
                saveCartLines(emptyList())
            }
            repository.deleteDraftOrder(orderId)
            refreshLocalSnapshots()
            _messages.tryEmit("Draf pesanan dihapus.")
        }
    }

    fun openReceipt(orderWithItems: OrderWithItems) {
        latestReceiptFlow.value = orderWithItems
    }

    fun dismissReceipt() {
        latestReceiptFlow.value = null
    }

    fun updateOrderStatus(orderWithItems: OrderWithItems, newStatus: OrderStatus) {
        val actor = uiState.value.currentUser?.name ?: "Kasir"
        viewModelScope.launch {
            repository.updateOrderStatus(orderWithItems, newStatus, actor)
            refreshLocalSnapshots()
            _messages.tryEmit("Status ${orderWithItems.order.orderNumber}: ${newStatus.label}")
        }
    }

    fun saveInventoryItem(
        item: InventoryItemEntity,
        selectedCategoryIds: List<Long>,
        recipeLinks: List<Pair<Long, Double>> = emptyList()
    ) {
        val actor = uiState.value.currentUser?.name ?: "Owner"
        viewModelScope.launch {
            repository.saveInventoryItemWithCategories(item, selectedCategoryIds, recipeLinks, actor)
            refreshLocalSnapshots()
            _messages.tryEmit("Item '${item.name}' tersimpan.")
        }
    }

    fun adjustStock(item: InventoryItemEntity, delta: Double, reason: String) {
        val actor = uiState.value.currentUser?.name ?: "Staf"
        viewModelScope.launch {
            repository.adjustStock(item, delta, reason, actor)
            refreshLocalSnapshots()
            _messages.tryEmit("Stok '${item.name}' diperbarui.")
        }
    }

    fun deleteInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
            refreshLocalSnapshots()
            _messages.tryEmit("Item '${item.name}' dihapus.")
        }
    }

    fun triggerLowStockNotificationCheck() {
        viewModelScope.launch {
            val count = repository.checkAndNotifyLowStock()
            if (count == 0) {
                _messages.tryEmit("Semua stok dalam kondisi aman.")
            } else {
                _messages.tryEmit("Ditemukan $count item dengan stok menipis/habis.")
            }
        }
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        analyticsPeriodFlow.value = period
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun cycleThemeMode() {
        val next = when (uiState.value.themeMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    fun updateStoreProfile(profile: StoreProfile) {
        viewModelScope.launch {
            repository.updateStoreProfile(profile)
            refreshLocalSnapshots()
            _messages.tryEmit("Profil toko, pajak & struk diperbarui.")
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
            refreshLocalSnapshots()
        }
    }

    fun runManualBackupNow() {
        viewModelScope.launch {
            repository.performLocalSnapshotNow()
            refreshLocalSnapshots()
            _messages.tryEmit("Backup lokal berhasil dibuat.")
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportBackupToUri(uri)
                _messages.tryEmit("File backup JSON berhasil diekspor.")
            } catch (e: Exception) {
                _messages.tryEmit("Ekspor backup gagal: ${e.message}")
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val (invCount, orderCount) = repository.restoreBackupFromUri(uri)
                refreshLocalSnapshots()
                _messages.tryEmit("Pemulihan berhasil: $invCount item stok & $orderCount pesanan.")
            } catch (e: Exception) {
                _messages.tryEmit("Pemulihan gagal: ${e.message}")
            }
        }
    }

    fun restoreLocalSnapshot(filePath: String) {
        viewModelScope.launch {
            try {
                val (invCount, orderCount) = repository.restoreFromLocalSnapshot(filePath)
                refreshLocalSnapshots()
                _messages.tryEmit("Snapshot dipulihkan ($invCount stok, $orderCount pesanan).")
            } catch (e: Exception) {
                _messages.tryEmit("Gagal memulihkan snapshot: ${e.message}")
            }
        }
    }

    fun exportExcel(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportExcelXml(uri)
                _messages.tryEmit("Laporan Excel (.xls) berhasil diekspor.")
            } catch (e: Exception) {
                _messages.tryEmit("Gagal ekspor Excel: ${e.message}")
            }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportSalesCsv(uri)
                _messages.tryEmit("Data penjualan CSV berhasil diekspor.")
            } catch (e: Exception) {
                _messages.tryEmit("Gagal ekspor CSV: ${e.message}")
            }
        }
    }

    fun exportPdf(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportPdfReport(uri)
                _messages.tryEmit("Laporan bisnis PDF berhasil diekspor.")
            } catch (e: Exception) {
                _messages.tryEmit("Gagal ekspor PDF: ${e.message}")
            }
        }
    }

    fun exportTemplateCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                repository.exportInventoryTemplateCsv(uri)
                _messages.tryEmit("Template impor CSV berhasil disimpan.")
            } catch (e: Exception) {
                _messages.tryEmit("Gagal menyimpan template: ${e.message}")
            }
        }
    }

    fun importInventoryCsv(uri: Uri) {
        val actor = uiState.value.currentUser?.name ?: "Owner"
        viewModelScope.launch {
            try {
                val res = repository.importInventoryCsv(uri, actor)
                refreshLocalSnapshots()
                if (res.errorSummary != null) {
                    _messages.tryEmit(res.errorSummary)
                } else {
                    _messages.tryEmit(
                        "Impor selesai: ${res.importedItems.size} item masuk" +
                            if (res.skippedRows > 0) " (${res.skippedRows} baris dilewati)." else "."
                    )
                }
            } catch (e: Exception) {
                _messages.tryEmit("Gagal impor CSV: ${e.message}")
            }
        }
    }

    private suspend fun saveCartLines(lines: List<CartLineItem>) {
        val json = if (lines.isEmpty()) "" else cartAdapter.toJson(lines)
        repository.saveDraftCartJson(json)
    }

    private fun parseCartLines(raw: String): List<CartLineItem> {
        if (raw.isBlank()) return emptyList()
        return try {
            cartAdapter.fromJson(raw).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun computeAnalyticsSnapshot(
            allOrders: List<OrderWithItems>,
            inventoryWithCategories: List<InventoryItemWithCategories>,
            allExpenses: List<ExpenseEntity>,
            period: AnalyticsPeriod
        ): AnalyticsSnapshot {
            val cal = Calendar.getInstance()
            val nowMillis = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            val cutoff = when (period) {
                AnalyticsPeriod.TODAY -> startOfToday
                AnalyticsPeriod.WEEK -> startOfToday - 6L * 24 * 60 * 60 * 1000
                AnalyticsPeriod.MONTH -> startOfToday - 29L * 24 * 60 * 60 * 1000
            }

            val validOrders = allOrders.filter {
                it.order.status != OrderStatus.CANCELLED.name &&
                    it.order.status != OrderStatus.DRAFT.name &&
                    it.order.createdAt >= cutoff
            }
            val periodExpenses = allExpenses.filter { it.expenseDate >= cutoff }

            val totalRevenue = validOrders.sumOf { it.order.totalAmount }
            val totalOrders = validOrders.size
            val aov = if (totalOrders > 0) totalRevenue / totalOrders else 0L
            val totalCost = validOrders.sumOf { owi -> owi.items.sumOf { it.unitCost * it.quantity } }
            val grossProfit = totalRevenue - totalCost
            val totalExpenses = periodExpenses.sumOf { it.amount }
            val netProfit = grossProfit - totalExpenses

            val chartPoints = when (period) {
                AnalyticsPeriod.TODAY -> {
                    val buckets = listOf(0 to "00-06", 6 to "06-10", 10 to "10-14", 14 to "14-18", 18 to "18-22", 22 to "22-24")
                    buckets.mapIndexed { idx, (startHour, label) ->
                        val endHour = if (idx == buckets.lastIndex) 24 else buckets[idx + 1].first
                        val matching = validOrders.filter { owi ->
                            val c = Calendar.getInstance().apply { timeInMillis = owi.order.createdAt }
                            val h = c.get(Calendar.HOUR_OF_DAY)
                            h in startHour until endHour
                        }
                        DailySalesPoint(label, matching.sumOf { it.order.totalAmount }, matching.size)
                    }
                }
                AnalyticsPeriod.WEEK -> {
                    val dayFmt = SimpleDateFormat("EEE", Locale("in", "ID"))
                    (6 downTo 0).map { offset ->
                        val dayStart = startOfToday - offset * 24L * 60 * 60 * 1000
                        val dayEnd = dayStart + 24L * 60 * 60 * 1000
                        val matching = validOrders.filter { it.order.createdAt in dayStart until dayEnd }
                        DailySalesPoint(dayFmt.format(Date(dayStart)), matching.sumOf { it.order.totalAmount }, matching.size)
                    }
                }
                AnalyticsPeriod.MONTH -> {
                    (5 downTo 0).map { blockIdx ->
                        val blockEnd = nowMillis - blockIdx * 5L * 24 * 60 * 60 * 1000
                        val blockStart = blockEnd - 5L * 24 * 60 * 60 * 1000
                        val matching = validOrders.filter { it.order.createdAt in blockStart until blockEnd }
                        val fmt = SimpleDateFormat("dd/MM", Locale.US)
                        DailySalesPoint(fmt.format(Date(blockEnd)), matching.sumOf { it.order.totalAmount }, matching.size)
                    }
                }
            }

            val paymentBreakdowns = PaymentMethod.entries.map { method ->
                val matching = validOrders.filter { it.order.paymentMethod == method.name }
                val sum = matching.sumOf { it.order.totalAmount }
                val pct = if (totalRevenue > 0) (sum.toFloat() / totalRevenue.toFloat()) else 0f
                PaymentBreakdown(method = method, totalAmount = sum, count = matching.size, percentage = pct)
            }

            val itemCategoriesLookup = inventoryWithCategories.associate { entry ->
                entry.item.id to entry.categoryNames.ifEmpty { listOf("Umum") }
            }
            val catStatsMap = linkedMapOf<String, Pair<Int, Long>>()
            validOrders.forEach { owi ->
                owi.items.forEach { line ->
                    val cats = itemCategoriesLookup[line.inventoryItemId] ?: listOf("Umum")
                    cats.forEach { catName ->
                        val cur = catStatsMap[catName] ?: (0 to 0L)
                        catStatsMap[catName] = (cur.first + line.quantity) to (cur.second + line.lineTotal)
                    }
                }
            }
            val categoryTotalRev = catStatsMap.values.sumOf { it.second }.coerceAtLeast(1L)
            val categoryBreakdowns = catStatsMap.entries
                .map { (name, pair) ->
                    CategorySalesBreakdown(
                        categoryName = name,
                        qtySold = pair.first,
                        revenue = pair.second,
                        percentage = pair.second.toFloat() / categoryTotalRev.toFloat()
                    )
                }
                .sortedByDescending { it.revenue }

            val expenseBreakdowns = ExpenseCategory.entries.map { category ->
                val matching = periodExpenses.filter { it.category == category.name }
                val sum = matching.sumOf { it.amount }
                val pct = if (totalExpenses > 0L) (sum.toFloat() / totalExpenses.toFloat()) else 0f
                ExpenseCategoryBreakdown(
                    category = category,
                    totalAmount = sum,
                    count = matching.size,
                    percentage = pct
                )
            }

            val productMap = linkedMapOf<String, Pair<Int, Long>>()
            validOrders.forEach { owi ->
                owi.items.forEach { item ->
                    val displayName = if (item.optionsSummary.isNotBlank()) {
                        "${item.itemName} (${item.optionsSummary})"
                    } else {
                        item.itemName
                    }
                    val cur = productMap[displayName] ?: (0 to 0L)
                    productMap[displayName] = (cur.first + item.quantity) to (cur.second + item.lineTotal)
                }
            }
            val topProducts = productMap.entries
                .map { TopSellingProduct(it.key, it.value.first, it.value.second) }
                .sortedByDescending { it.qtySold }
                .take(5)

            return AnalyticsSnapshot(
                totalRevenue = totalRevenue,
                totalCost = totalCost,
                grossProfit = grossProfit,
                totalExpenses = totalExpenses,
                netProfit = netProfit,
                totalOrders = totalOrders,
                averageOrderValue = aov,
                chartPoints = chartPoints,
                paymentBreakdowns = paymentBreakdowns,
                categoryBreakdowns = categoryBreakdowns,
                expenseBreakdowns = expenseBreakdowns,
                topProducts = topProducts
            )
        }
    }
}
