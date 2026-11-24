package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationGroup
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.CustomizationOptionType
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryCategoryCrossRef
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.InventoryType
import com.example.data.local.KedaiKuDatabase
import com.example.data.local.MenuRecipeEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderStatus
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.local.StaffAccountEntity
import com.example.data.local.StaffRole
import com.example.data.local.StockMovementEntity
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.AppPrefsState
import com.example.data.prefs.StoreProfile
import com.example.data.security.PinSecurity
import com.example.ui.theme.ThemeMode
import com.example.util.BackupManager
import com.example.util.BackupManager.toEntity
import com.example.util.CsvImportResult
import com.example.util.LocalBackupInfo
import com.example.util.LowStockNotifier
import com.example.util.RecipeStockCalculator
import com.example.util.ReportExportImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class CheckoutLineInput(
    val item: InventoryItemEntity,
    val quantity: Int,
    val unitPrice: Long,
    val optionsSummary: String
)

data class CheckoutRequest(
    val existingDraftId: Long? = null,
    val cartLines: List<CheckoutLineInput>,
    val customerName: String,
    val orderType: OrderType,
    val paymentMethod: PaymentMethod,
    val discountAmount: Long,
    val paidAmount: Long,
    val notes: String,
    val cashier: StaffAccountEntity,
    val saveAsDraft: Boolean = false
)

class KedaiKuRepository(
    private val context: Context,
    private val db: KedaiKuDatabase,
    private val prefs: AppPreferences
) {
    private val staffDao = db.staffDao()
    private val categoryDao = db.categoryDao()
    private val customizationOptionDao = db.customizationOptionDao()
    private val expenseDao = db.expenseDao()
    private val inventoryDao = db.inventoryDao()
    private val menuRecipeDao = db.menuRecipeDao()
    private val orderDao = db.orderDao()
    private val stockMovementDao = db.stockMovementDao()

    val prefsFlow: Flow<AppPrefsState> = prefs.prefsFlow
    val allStaffFlow: Flow<List<StaffAccountEntity>> = staffDao.observeAllStaff()
    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()
    val allCustomizationOptionsFlow: Flow<List<CustomizationOptionEntity>> =
        customizationOptionDao.observeAllOptions()
    val allExpensesFlow: Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()
    val allInventoryWithCategoriesFlow: Flow<List<InventoryItemWithCategories>> =
        inventoryDao.observeAllInventoryWithCategories()
    val allRecipesFlow: Flow<List<MenuRecipeEntity>> = menuRecipeDao.observeAllRecipes()
    val allOrdersFlow: Flow<List<OrderWithItems>> = orderDao.observeAllOrdersWithItems()
    val recentMovementsFlow: Flow<List<StockMovementEntity>> = stockMovementDao.observeRecentMovements()

    suspend fun ensureDefaultCustomizationOptions() = withContext(Dispatchers.IO) {
        if (customizationOptionDao.countOptions() == 0) {
            customizationOptionDao.insertOptions(defaultCustomizationOptions())
        }
        ensureStarterRecipesIfEmpty()
    }

    private suspend fun ensureStarterRecipesIfEmpty() {
        if (menuRecipeDao.getAllRecipesSnapshot().isNotEmpty()) return
        val kop1 = inventoryDao.getBySku("KOP-001")?.id
        val kop2 = inventoryDao.getBySku("KOP-002")?.id
        val min3 = inventoryDao.getBySku("MIN-003")?.id
        val bhn1 = inventoryDao.getBySku("BHN-001")?.id
        val bhn2 = inventoryDao.getBySku("BHN-002")?.id
        val bhn3 = inventoryDao.getBySku("BHN-003")?.id
        val bhn4 = inventoryDao.getBySku("BHN-004")?.id

        val links = buildList {
            if (kop1 != null && bhn1 != null) add(MenuRecipeEntity(kop1, bhn1, 0.02))
            if (kop1 != null && bhn2 != null) add(MenuRecipeEntity(kop1, bhn2, 0.15))
            if (kop1 != null && bhn3 != null) add(MenuRecipeEntity(kop1, bhn3, 0.05))
            if (kop2 != null && bhn1 != null) add(MenuRecipeEntity(kop2, bhn1, 0.02))
            if (min3 != null && bhn4 != null) add(MenuRecipeEntity(min3, bhn4, 0.02))
            if (min3 != null && bhn2 != null) add(MenuRecipeEntity(min3, bhn2, 0.15))
        }
        if (links.isNotEmpty()) {
            menuRecipeDao.insertRecipes(links)
        }
    }

    private fun defaultCustomizationOptions(): List<CustomizationOptionEntity> = listOf(
        // Drink Variants
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_VARIANT.name, label = "Regular", extraPrice = 0L, sortOrder = 1),
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_VARIANT.name, label = "Large", extraPrice = 5000L, sortOrder = 2),
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_VARIANT.name, label = "Oat Milk", extraPrice = 7000L, sortOrder = 3),
        // Drink Toppings
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_TOPPING.name, label = "Pearl Boba", extraPrice = 4000L, sortOrder = 1),
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_TOPPING.name, label = "Cream Cheese", extraPrice = 6000L, sortOrder = 2),
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_TOPPING.name, label = "Grass Jelly", extraPrice = 3500L, sortOrder = 3),
        CustomizationOptionEntity(optionType = CustomizationOptionType.DRINK_TOPPING.name, label = "Espresso Shot", extraPrice = 5000L, sortOrder = 4),
        // Food Spice Levels 1-5
        CustomizationOptionEntity(optionType = CustomizationOptionType.FOOD_LEVEL.name, label = "Lv 1 (Tidak Pedas)", extraPrice = 0L, sortOrder = 1),
        CustomizationOptionEntity(optionType = CustomizationOptionType.FOOD_LEVEL.name, label = "Lv 2 (Sedang)", extraPrice = 0L, sortOrder = 2),
        CustomizationOptionEntity(optionType = CustomizationOptionType.FOOD_LEVEL.name, label = "Lv 3 (Pedas)", extraPrice = 1000L, sortOrder = 3),
        CustomizationOptionEntity(optionType = CustomizationOptionType.FOOD_LEVEL.name, label = "Lv 4 (Ekstra Pedas)", extraPrice = 2000L, sortOrder = 4),
        CustomizationOptionEntity(optionType = CustomizationOptionType.FOOD_LEVEL.name, label = "Lv 5 (Super Pedas)", extraPrice = 3000L, sortOrder = 5)
    )

    suspend fun setupInitialOwner(
        storeName: String,
        ownerName: String,
        pin: String,
        seedStarterMenu: Boolean
    ): StaffAccountEntity = withContext(Dispatchers.IO) {
        val cleanName = ownerName.trim().ifEmpty { "Pemilik Toko" }
        val cleanStore = storeName.trim().ifEmpty { "KedaiKu Bistro & Kopi" }
        val salt = PinSecurity.generateSalt()
        val hash = PinSecurity.hashPin(pin, salt)

        val owner = StaffAccountEntity(
            name = cleanName,
            username = cleanName.lowercase().replace(" ", "_"),
            role = StaffRole.OWNER.name,
            pinSalt = salt,
            pinHash = hash,
            isActive = true
        )

        val ownerId = db.withTransaction {
            val id = staffDao.insertStaff(owner)
            if (customizationOptionDao.countOptions() == 0) {
                customizationOptionDao.insertOptions(defaultCustomizationOptions())
            }
            if (seedStarterMenu && inventoryDao.countItems() == 0) {
                seedStarterCatalogInternal()
            }
            id
        }

        val currentPrefs = prefsFlow.first()
        prefs.updateStoreProfile(currentPrefs.storeProfile.copy(storeName = cleanStore))
        prefs.setActiveStaffId(ownerId)
        triggerAutoBackupIfEnabled()
        staffDao.getStaffById(ownerId) ?: owner.copy(id = ownerId)
    }

    private suspend fun seedStarterCatalogInternal() {
        val now = System.currentTimeMillis()
        val catKopi = categoryDao.insertCategory(CategoryEntity(name = "Kopi & Minuman", description = "Aneka kopi, teh, dan minuman segar", createdAt = now))
        val catMakanan = categoryDao.insertCategory(CategoryEntity(name = "Makanan Utama", description = "Hidangan berat & rice bowl", createdAt = now))
        val catPastry = categoryDao.insertCategory(CategoryEntity(name = "Pastry & Snack", description = "Camilan & roti panggang", createdAt = now))
        val catBestSeller = categoryDao.insertCategory(CategoryEntity(name = "Best Seller", description = "Menu favorit pelanggan", createdAt = now))
        val catBahan = categoryDao.insertCategory(CategoryEntity(name = "Bahan Baku", description = "Stok bahan dapur & bar", createdAt = now))

        val itemsWithCats = listOf(
            InventoryItemEntity(
                sku = "KOP-001", name = "Kopi Susu Gula Aren", category = "Kopi & Minuman, Best Seller",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.DRINK.name,
                price = 24000, costPrice = 8500, stockQty = 35.0, minThreshold = 8.0, unit = "cup", updatedAt = now
            ) to listOf(catKopi, catBestSeller),
            InventoryItemEntity(
                sku = "KOP-002", name = "Americano Dingin", category = "Kopi & Minuman",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.DRINK.name,
                price = 20000, costPrice = 6000, stockQty = 40.0, minThreshold = 8.0, unit = "cup", updatedAt = now
            ) to listOf(catKopi),
            InventoryItemEntity(
                sku = "MIN-003", name = "Matcha Latte Artisan", category = "Kopi & Minuman, Best Seller",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.DRINK.name,
                price = 28000, costPrice = 11000, stockQty = 20.0, minThreshold = 6.0, unit = "cup", updatedAt = now
            ) to listOf(catKopi, catBestSeller),
            InventoryItemEntity(
                sku = "MIN-004", name = "Teh Tarik Melaka", category = "Kopi & Minuman",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.DRINK.name,
                price = 18000, costPrice = 5500, stockQty = 28.0, minThreshold = 6.0, unit = "cup", updatedAt = now
            ) to listOf(catKopi),
            InventoryItemEntity(
                sku = "MKN-001", name = "Nasi Goreng Kampung", category = "Makanan Utama, Best Seller",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.FOOD.name,
                price = 35000, costPrice = 15000, stockQty = 18.0, minThreshold = 5.0, unit = "porsi", updatedAt = now
            ) to listOf(catMakanan, catBestSeller),
            InventoryItemEntity(
                sku = "MKN-002", name = "Rice Bowl Ayam Sambal Matah", category = "Makanan Utama",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.FOOD.name,
                price = 38000, costPrice = 16500, stockQty = 15.0, minThreshold = 5.0, unit = "porsi", updatedAt = now
            ) to listOf(catMakanan),
            InventoryItemEntity(
                sku = "SNK-001", name = "Butter Croissant", category = "Pastry & Snack",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.NONE.name,
                price = 22000, costPrice = 9500, stockQty = 6.0, minThreshold = 5.0, unit = "pcs", updatedAt = now
            ) to listOf(catPastry),
            InventoryItemEntity(
                sku = "SNK-002", name = "Pisang Goreng Keju Karamel", category = "Pastry & Snack",
                itemType = InventoryType.MENU.name, customizationGroup = CustomizationGroup.FOOD.name,
                price = 25000, costPrice = 10000, stockQty = 14.0, minThreshold = 4.0, unit = "porsi", updatedAt = now
            ) to listOf(catPastry),
            InventoryItemEntity(
                sku = "BHN-001", name = "Biji Kopi Arabika Blend", category = "Bahan Baku",
                itemType = InventoryType.INGREDIENT.name, customizationGroup = CustomizationGroup.NONE.name,
                price = 0, costPrice = 165000, stockQty = 4.5, minThreshold = 2.0, unit = "kg", updatedAt = now
            ) to listOf(catBahan),
            InventoryItemEntity(
                sku = "BHN-002", name = "Susu Segar UHT Full Cream", category = "Bahan Baku",
                itemType = InventoryType.INGREDIENT.name, customizationGroup = CustomizationGroup.NONE.name,
                price = 0, costPrice = 21000, stockQty = 4.0, minThreshold = 6.0, unit = "liter", updatedAt = now
            ) to listOf(catBahan),
            InventoryItemEntity(
                sku = "BHN-003", name = "Gula Aren Cair Premium", category = "Bahan Baku",
                itemType = InventoryType.INGREDIENT.name, customizationGroup = CustomizationGroup.NONE.name,
                price = 0, costPrice = 38000, stockQty = 5.0, minThreshold = 2.0, unit = "botol", updatedAt = now
            ) to listOf(catBahan),
            InventoryItemEntity(
                sku = "BHN-004", name = "Bubuk Matcha Jepang", category = "Bahan Baku",
                itemType = InventoryType.INGREDIENT.name, customizationGroup = CustomizationGroup.NONE.name,
                price = 0, costPrice = 240000, stockQty = 1.0, minThreshold = 0.25, unit = "kg", updatedAt = now
            ) to listOf(catBahan)
        )

        val insertedBySku = mutableMapOf<String, Long>()
        itemsWithCats.forEach { (item, catIds) ->
            val id = inventoryDao.insertItem(item)
            insertedBySku[item.sku] = id
            categoryDao.insertCrossRefs(catIds.map { catId -> InventoryCategoryCrossRef(id, catId) })
        }

        val recipeLinks = listOfNotNull(
            insertedBySku["KOP-001"]?.let { m -> insertedBySku["BHN-001"]?.let { b -> MenuRecipeEntity(m, b, 0.02) } },
            insertedBySku["KOP-001"]?.let { m -> insertedBySku["BHN-002"]?.let { b -> MenuRecipeEntity(m, b, 0.15) } },
            insertedBySku["KOP-001"]?.let { m -> insertedBySku["BHN-003"]?.let { b -> MenuRecipeEntity(m, b, 0.05) } },
            insertedBySku["KOP-002"]?.let { m -> insertedBySku["BHN-001"]?.let { b -> MenuRecipeEntity(m, b, 0.02) } },
            insertedBySku["MIN-003"]?.let { m -> insertedBySku["BHN-004"]?.let { b -> MenuRecipeEntity(m, b, 0.02) } },
            insertedBySku["MIN-003"]?.let { m -> insertedBySku["BHN-002"]?.let { b -> MenuRecipeEntity(m, b, 0.15) } }
        )
        if (recipeLinks.isNotEmpty()) {
            menuRecipeDao.insertRecipes(recipeLinks)
        }
    }

    suspend fun verifyPinAndLogin(staffId: Long, pin: String): StaffAccountEntity? =
        withContext(Dispatchers.IO) {
            val staff = staffDao.getStaffById(staffId) ?: return@withContext null
            if (!staff.isActive) return@withContext null
            if (PinSecurity.verifyPin(pin, staff.pinSalt, staff.pinHash)) {
                prefs.setActiveStaffId(staff.id)
                staff
            } else {
                null
            }
        }

    suspend fun logout() {
        prefs.setActiveStaffId(-1L)
    }

    suspend fun addStaffAccount(name: String, role: StaffRole, pin: String): Long =
        withContext(Dispatchers.IO) {
            val salt = PinSecurity.generateSalt()
            val hash = PinSecurity.hashPin(pin, salt)
            val entity = StaffAccountEntity(
                name = name.trim(),
                username = name.trim().lowercase().replace(" ", "_"),
                role = role.name,
                pinSalt = salt,
                pinHash = hash,
                isActive = true
            )
            staffDao.insertStaff(entity)
        }

    suspend fun toggleStaffActive(staff: StaffAccountEntity) = withContext(Dispatchers.IO) {
        staffDao.updateStaff(staff.copy(isActive = !staff.isActive))
    }

    // Category Management (Owner)
    suspend fun saveCategory(id: Long, name: String, description: String): CategoryEntity =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            val existingByName = categoryDao.getByName(trimmed)
            val targetId = if (id > 0) id else (existingByName?.id ?: 0L)
            val entity = CategoryEntity(
                id = targetId,
                name = trimmed,
                description = description.trim(),
                createdAt = existingByName?.createdAt ?: System.currentTimeMillis()
            )
            val savedId = if (targetId > 0) {
                categoryDao.updateCategory(entity)
                targetId
            } else {
                categoryDao.insertCategory(entity)
            }
            syncLegacyCategoryLabelsForAllItems()
            triggerAutoBackupIfEnabled()
            entity.copy(id = savedId)
        }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            categoryDao.deleteCategory(category)
            syncLegacyCategoryLabelsForAllItems()
        }
        triggerAutoBackupIfEnabled()
    }

    private suspend fun syncLegacyCategoryLabelsForAllItems() {
        val allWithCats = inventoryDao.getAllInventoryWithCategoriesSnapshot()
        allWithCats.forEach { entry ->
            val joined = entry.categories.joinToString(", ") { it.name }.ifBlank { "Umum" }
            if (entry.item.category != joined) {
                inventoryDao.updateItem(entry.item.copy(category = joined))
            }
        }
    }

    // Customization Option Price Management (Owner)
    suspend fun saveCustomizationOption(option: CustomizationOptionEntity) = withContext(Dispatchers.IO) {
        if (option.id > 0) {
            customizationOptionDao.updateOption(option)
        } else {
            customizationOptionDao.insertOption(option)
        }
        triggerAutoBackupIfEnabled()
    }

    suspend fun deleteCustomizationOption(option: CustomizationOptionEntity) = withContext(Dispatchers.IO) {
        customizationOptionDao.deleteOption(option)
        triggerAutoBackupIfEnabled()
    }

    // Expense Management (Owner)
    suspend fun saveExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        if (expense.id > 0) {
            expenseDao.updateExpense(expense)
        } else {
            expenseDao.insertExpense(expense)
        }
        triggerAutoBackupIfEnabled()
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
        triggerAutoBackupIfEnabled()
    }

    suspend fun saveInventoryItemWithCategories(
        item: InventoryItemEntity,
        selectedCategoryIds: List<Long>,
        recipeLinks: List<Pair<Long, Double>>,
        actorName: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val allCats = categoryDao.getAllCategoriesSnapshot()
            val validCatIds = selectedCategoryIds.filter { id -> allCats.any { it.id == id } }
            val catNames = allCats.filter { it.id in validCatIds }.joinToString(", ") { it.name }
                .ifBlank { item.category.ifBlank { "Umum" } }

            val existing = if (item.id > 0) inventoryDao.getById(item.id) else null
            val savedId = inventoryDao.insertItem(
                item.copy(category = catNames, updatedAt = now)
            )
            categoryDao.deleteCrossRefsForItem(savedId)
            if (validCatIds.isNotEmpty()) {
                categoryDao.insertCrossRefs(validCatIds.map { InventoryCategoryCrossRef(savedId, it) })
            }

            if (item.itemType == InventoryType.MENU.name) {
                menuRecipeDao.deleteRecipesForIngredient(savedId)
                menuRecipeDao.deleteRecipesForMenu(savedId)
                val validRecipes = recipeLinks
                    .filter { (ingId, qty) -> ingId != savedId && qty > 0.0 }
                    .distinctBy { it.first }
                    .map { (ingId, qty) ->
                        MenuRecipeEntity(menuItemId = savedId, ingredientItemId = ingId, quantityRequired = qty)
                    }
                if (validRecipes.isNotEmpty()) {
                    menuRecipeDao.insertRecipes(validRecipes)
                }
            } else {
                menuRecipeDao.deleteRecipesForMenu(savedId)
                menuRecipeDao.deleteRecipesForIngredient(savedId)
                val validRecipes = recipeLinks
                    .filter { (menuId, qty) -> menuId != savedId && qty > 0.0 }
                    .distinctBy { it.first }
                    .map { (menuId, qty) ->
                        MenuRecipeEntity(menuItemId = menuId, ingredientItemId = savedId, quantityRequired = qty)
                    }
                if (validRecipes.isNotEmpty()) {
                    menuRecipeDao.insertRecipes(validRecipes)
                }
            }

            if (existing == null) {
                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        inventoryItemId = savedId,
                        itemName = item.name,
                        delta = item.stockQty,
                        previousQty = 0.0,
                        newQty = item.stockQty,
                        reason = "Item baru ditambahkan",
                        actorName = actorName,
                        createdAt = now
                    )
                )
            } else if (existing.stockQty != item.stockQty) {
                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        inventoryItemId = savedId,
                        itemName = item.name,
                        delta = item.stockQty - existing.stockQty,
                        previousQty = existing.stockQty,
                        newQty = item.stockQty,
                        reason = "Pembaruan stok item",
                        actorName = actorName,
                        createdAt = now
                    )
                )
            }
        }
        checkAndNotifyLowStock()
        triggerAutoBackupIfEnabled()
    }

    suspend fun adjustStock(
        item: InventoryItemEntity,
        delta: Double,
        reason: String,
        actorName: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val latest = inventoryDao.getById(item.id) ?: item
            val newQty = (latest.stockQty + delta).coerceAtLeast(0.0)
            inventoryDao.updateItem(latest.copy(stockQty = newQty, updatedAt = now))
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    inventoryItemId = latest.id,
                    itemName = latest.name,
                    delta = newQty - latest.stockQty,
                    previousQty = latest.stockQty,
                    newQty = newQty,
                    reason = reason.ifBlank { if (delta >= 0) "Stok masuk" else "Penyesuaian stok" },
                    actorName = actorName,
                    createdAt = now
                )
            )
        }
        checkAndNotifyLowStock()
        triggerAutoBackupIfEnabled()
    }

    suspend fun deleteInventoryItem(item: InventoryItemEntity) = withContext(Dispatchers.IO) {
        inventoryDao.deleteItem(item)
        triggerAutoBackupIfEnabled()
    }

    suspend fun placeOrSaveOrder(request: CheckoutRequest): OrderWithItems = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val prefsState = prefsFlow.first()
        val taxPercent = prefsState.storeProfile.taxPercentFor(request.orderType)

        val subtotal = request.cartLines.sumOf { it.unitPrice * it.quantity }
        val discount = request.discountAmount.coerceIn(0L, subtotal)
        val taxableBase = (subtotal - discount).coerceAtLeast(0L)
        val taxAmount = (taxableBase * taxPercent) / 100L
        val total = taxableBase + taxAmount
        val paid = if (request.saveAsDraft) {
            0L
        } else if (request.paymentMethod == PaymentMethod.TUNAI) {
            request.paidAmount.coerceAtLeast(total)
        } else {
            total
        }
        val change = if (request.saveAsDraft) 0L else (paid - total).coerceAtLeast(0L)

        val orderId = db.withTransaction {
            val existingDraft = request.existingDraftId?.let { orderDao.getOrderWithItemsById(it)?.order }
            val orderNumber = if (existingDraft != null) {
                if (request.saveAsDraft) {
                    existingDraft.orderNumber
                } else {
                    val count = orderDao.countOrders()
                    "ORD-%04d".format(count)
                }
            } else {
                val count = orderDao.countOrders() + 1
                if (request.saveAsDraft) "DRF-%04d".format(count) else "ORD-%04d".format(count)
            }

            val orderStatus = if (request.saveAsDraft) OrderStatus.DRAFT.name else OrderStatus.PENDING.name
            val orderEntity = OrderEntity(
                id = existingDraft?.id ?: 0L,
                orderNumber = orderNumber,
                customerName = request.customerName.trim(),
                orderType = request.orderType.name,
                status = orderStatus,
                paymentMethod = request.paymentMethod.name,
                subtotal = subtotal,
                discountAmount = discount,
                taxAmount = taxAmount,
                totalAmount = total,
                paidAmount = paid,
                changeAmount = change,
                notes = request.notes.trim(),
                cashierId = request.cashier.id,
                cashierName = request.cashier.name,
                createdAt = existingDraft?.createdAt ?: now,
                updatedAt = now
            )

            val savedOrderId = if (existingDraft != null) {
                orderDao.updateOrder(orderEntity)
                orderDao.deleteOrderItems(existingDraft.id)
                existingDraft.id
            } else {
                orderDao.insertOrder(orderEntity)
            }

            val orderItems = request.cartLines.map { line ->
                OrderItemEntity(
                    orderId = savedOrderId,
                    inventoryItemId = line.item.id,
                    itemName = line.item.name,
                    optionsSummary = line.optionsSummary,
                    unitPrice = line.unitPrice,
                    unitCost = line.item.costPrice,
                    quantity = line.quantity,
                    lineTotal = line.unitPrice * line.quantity
                )
            }
            orderDao.insertOrderItems(orderItems)

            if (!request.saveAsDraft) {
                request.cartLines.forEach { line ->
                    val recipes = menuRecipeDao.getRecipesForMenu(line.item.id)
                        .filter { it.quantityRequired > 0.0 }
                    if (recipes.isNotEmpty()) {
                        recipes.forEach { recipe ->
                            val ingredient = inventoryDao.getById(recipe.ingredientItemId)
                            if (ingredient != null) {
                                val consumeQty = recipe.quantityRequired * line.quantity
                                val newStock = (ingredient.stockQty - consumeQty).coerceAtLeast(0.0)
                                inventoryDao.updateItem(ingredient.copy(stockQty = newStock, updatedAt = now))
                                stockMovementDao.insertMovement(
                                    StockMovementEntity(
                                        inventoryItemId = ingredient.id,
                                        itemName = ingredient.name,
                                        delta = newStock - ingredient.stockQty,
                                        previousQty = ingredient.stockQty,
                                        newQty = newStock,
                                        reason = "Resep ${line.item.name} ($orderNumber)",
                                        actorName = request.cashier.name,
                                        createdAt = now
                                    )
                                )
                            }
                        }
                    } else {
                        val current = inventoryDao.getById(line.item.id)
                        if (current != null) {
                            val newStock = (current.stockQty - line.quantity).coerceAtLeast(0.0)
                            inventoryDao.updateItem(current.copy(stockQty = newStock, updatedAt = now))
                            stockMovementDao.insertMovement(
                                StockMovementEntity(
                                    inventoryItemId = current.id,
                                    itemName = current.name,
                                    delta = newStock - current.stockQty,
                                    previousQty = current.stockQty,
                                    newQty = newStock,
                                    reason = "Penjualan $orderNumber",
                                    actorName = request.cashier.name,
                                    createdAt = now
                                )
                            )
                        }
                    }
                }
            }
            savedOrderId
        }

        prefs.saveDraftCartJson("")
        if (!request.saveAsDraft) {
            checkAndNotifyLowStock()
        }
        triggerAutoBackupIfEnabled()

        orderDao.getOrderWithItemsById(orderId)
            ?: error("Pesanan tersimpan tetapi gagal memuat detail.")
    }

    suspend fun deleteDraftOrder(orderId: Long) = withContext(Dispatchers.IO) {
        orderDao.deleteOrderById(orderId)
        triggerAutoBackupIfEnabled()
    }

    suspend fun updateOrderStatus(orderWithItems: OrderWithItems, newStatus: OrderStatus, actorName: String) =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val previousStatus = orderWithItems.order.status
            db.withTransaction {
                orderDao.updateOrderStatus(orderWithItems.order.id, newStatus.name, now)
                if (newStatus == OrderStatus.CANCELLED &&
                    previousStatus != OrderStatus.CANCELLED.name &&
                    previousStatus != OrderStatus.DRAFT.name
                ) {
                    orderWithItems.items.forEach { line ->
                        val recipes = menuRecipeDao.getRecipesForMenu(line.inventoryItemId)
                            .filter { it.quantityRequired > 0.0 }
                        if (recipes.isNotEmpty()) {
                            recipes.forEach { recipe ->
                                val ingredient = inventoryDao.getById(recipe.ingredientItemId)
                                if (ingredient != null) {
                                    val restoreQty = recipe.quantityRequired * line.quantity
                                    val restored = ingredient.stockQty + restoreQty
                                    inventoryDao.updateItem(ingredient.copy(stockQty = restored, updatedAt = now))
                                    stockMovementDao.insertMovement(
                                        StockMovementEntity(
                                            inventoryItemId = ingredient.id,
                                            itemName = ingredient.name,
                                            delta = restoreQty,
                                            previousQty = ingredient.stockQty,
                                            newQty = restored,
                                            reason = "Batal ${orderWithItems.order.orderNumber} (${line.itemName})",
                                            actorName = actorName,
                                            createdAt = now
                                        )
                                    )
                                }
                            }
                        } else {
                            val inv = inventoryDao.getById(line.inventoryItemId)
                            if (inv != null) {
                                val restored = inv.stockQty + line.quantity
                                inventoryDao.updateItem(inv.copy(stockQty = restored, updatedAt = now))
                                stockMovementDao.insertMovement(
                                    StockMovementEntity(
                                        inventoryItemId = inv.id,
                                        itemName = inv.name,
                                        delta = line.quantity.toDouble(),
                                        previousQty = inv.stockQty,
                                        newQty = restored,
                                        reason = "Batal pesanan ${orderWithItems.order.orderNumber}",
                                        actorName = actorName,
                                        createdAt = now
                                    )
                                )
                            }
                        }
                    }
                }
            }
            checkAndNotifyLowStock()
            triggerAutoBackupIfEnabled()
        }

    suspend fun checkAndNotifyLowStock(): Int = withContext(Dispatchers.IO) {
        val all = inventoryDao.getAllInventorySnapshot()
        val itemsById = all.associateBy { it.id }
        val recipesByMenu = menuRecipeDao.getAllRecipesSnapshot().groupBy { it.menuItemId }
        val low = all.filter { item ->
            val effective = RecipeStockCalculator.computeEffectiveStock(item, recipesByMenu, itemsById)
            effective <= item.minThreshold
        }
        LowStockNotifier.notifyLowStock(context, low)
        low.size
    }

    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)

    suspend fun updateStoreProfile(profile: StoreProfile) {
        prefs.updateStoreProfile(profile)
        triggerAutoBackupIfEnabled()
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.setAutoBackupEnabled(enabled)
        if (enabled) triggerAutoBackupIfEnabled()
    }

    suspend fun saveDraftCartJson(json: String) {
        prefs.saveDraftCartJson(json)
    }

    suspend fun triggerAutoBackupIfEnabled() = withContext(Dispatchers.IO) {
        val prefsState = prefsFlow.first()
        if (!prefsState.autoBackupEnabled) return@withContext
        performLocalSnapshotNow()
    }

    suspend fun performLocalSnapshotNow(): Long = withContext(Dispatchers.IO) {
        val prefsState = prefsFlow.first()
        val json = BackupManager.buildPayloadJson(
            store = prefsState.storeProfile,
            categories = categoryDao.getAllCategoriesSnapshot(),
            categoryRefs = categoryDao.getAllCrossRefsSnapshot(),
            menuRecipes = menuRecipeDao.getAllRecipesSnapshot(),
            customizationOptions = customizationOptionDao.getAllOptionsSnapshot(),
            expenses = expenseDao.getAllExpensesSnapshot(),
            inventory = inventoryDao.getAllInventorySnapshot(),
            orders = orderDao.getAllOrdersWithItemsSnapshot(),
            movements = stockMovementDao.getAllMovementsSnapshot()
        )
        val timestamp = BackupManager.writeAutoBackupSnapshot(context, json)
        prefs.updateLastAutoBackupTime(timestamp)
        timestamp
    }

    suspend fun getLocalBackupSnapshots(): List<LocalBackupInfo> = withContext(Dispatchers.IO) {
        BackupManager.listLocalSnapshots(context)
    }

    suspend fun exportBackupToUri(uri: Uri) = withContext(Dispatchers.IO) {
        val prefsState = prefsFlow.first()
        val json = BackupManager.buildPayloadJson(
            store = prefsState.storeProfile,
            categories = categoryDao.getAllCategoriesSnapshot(),
            categoryRefs = categoryDao.getAllCrossRefsSnapshot(),
            menuRecipes = menuRecipeDao.getAllRecipesSnapshot(),
            customizationOptions = customizationOptionDao.getAllOptionsSnapshot(),
            expenses = expenseDao.getAllExpensesSnapshot(),
            inventory = inventoryDao.getAllInventorySnapshot(),
            orders = orderDao.getAllOrdersWithItemsSnapshot(),
            movements = stockMovementDao.getAllMovementsSnapshot()
        )
        BackupManager.exportToUri(context, uri, json)
    }

    suspend fun restoreBackupFromUri(uri: Uri): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val payload = BackupManager.readFromUri(context, uri)
        restorePayload(payload)
    }

    suspend fun restoreFromLocalSnapshot(filePath: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val payload = BackupManager.readSnapshotFile(filePath)
            ?: error("Snapshot backup lokal tidak ditemukan atau rusak.")
        restorePayload(payload)
    }

    private suspend fun restorePayload(payload: com.example.util.KedaiKuBackupPayload): Pair<Int, Int> {
        db.withTransaction {
            orderDao.deleteAllOrders()
            stockMovementDao.deleteAll()
            menuRecipeDao.deleteAllRecipes()
            categoryDao.deleteAllCrossRefs()
            categoryDao.deleteAllCategories()
            customizationOptionDao.deleteAllOptions()
            expenseDao.deleteAllExpenses()
            inventoryDao.deleteAll()

            payload.categories.forEach { categoryDao.insertCategory(it.toEntity()) }
            if (payload.customizationOptions.isNotEmpty()) {
                customizationOptionDao.insertOptions(payload.customizationOptions.map { it.toEntity() })
            } else {
                customizationOptionDao.insertOptions(defaultCustomizationOptions())
            }
            payload.expenses.forEach { expenseDao.insertExpense(it.toEntity()) }
            inventoryDao.insertItems(payload.inventory.map { it.toEntity() })
            if (payload.categoryRefs.isNotEmpty()) {
                categoryDao.insertCrossRefs(payload.categoryRefs.map { it.toEntity() })
            }
            if (payload.menuRecipes.isNotEmpty()) {
                menuRecipeDao.insertRecipes(payload.menuRecipes.map { it.toEntity() })
            }
            payload.orders.forEach { orderDto ->
                orderDao.insertOrder(orderDto.toEntity())
                orderDao.insertOrderItems(orderDto.items.map { it.toEntity() })
            }
            stockMovementDao.insertMovements(payload.stockMovements.map { it.toEntity() })
        }
        prefs.updateStoreProfile(
            StoreProfile(
                storeName = payload.storeProfile.storeName,
                storeAddress = payload.storeProfile.storeAddress,
                storePhone = payload.storeProfile.storePhone,
                receiptFooter = payload.storeProfile.receiptFooter,
                taxTakeAwayPercent = payload.storeProfile.taxTakeAwayPercent,
                taxShopeePercent = payload.storeProfile.taxShopeePercent,
                taxGrabPercent = payload.storeProfile.taxGrabPercent
            )
        )
        checkAndNotifyLowStock()
        return payload.inventory.size to payload.orders.size
    }

    suspend fun exportExcelXml(uri: Uri) = withContext(Dispatchers.IO) {
        val prefsState = prefsFlow.first()
        ReportExportImport.exportSalesAndInventoryExcelXml(
            context = context,
            uri = uri,
            store = prefsState.storeProfile,
            orders = orderDao.getAllOrdersWithItemsSnapshot(),
            inventory = inventoryDao.getAllInventoryWithCategoriesSnapshot(),
            expenses = expenseDao.getAllExpensesSnapshot()
        )
    }

    suspend fun exportSalesCsv(uri: Uri) = withContext(Dispatchers.IO) {
        ReportExportImport.exportSalesCsv(
            context = context,
            uri = uri,
            orders = orderDao.getAllOrdersWithItemsSnapshot()
        )
    }

    suspend fun exportPdfReport(uri: Uri) = withContext(Dispatchers.IO) {
        val prefsState = prefsFlow.first()
        ReportExportImport.exportBusinessReportPdf(
            context = context,
            uri = uri,
            store = prefsState.storeProfile,
            orders = orderDao.getAllOrdersWithItemsSnapshot(),
            inventory = inventoryDao.getAllInventoryWithCategoriesSnapshot(),
            expenses = expenseDao.getAllExpensesSnapshot()
        )
    }

    suspend fun exportInventoryTemplateCsv(uri: Uri) = withContext(Dispatchers.IO) {
        ReportExportImport.exportInventoryTemplateCsv(context, uri)
    }

    suspend fun importInventoryCsv(uri: Uri, actorName: String): CsvImportResult =
        withContext(Dispatchers.IO) {
            val parsed = ReportExportImport.parseInventoryCsv(context, uri)
            if (parsed.importedItems.isNotEmpty()) {
                val now = System.currentTimeMillis()
                db.withTransaction {
                    parsed.importedItems.forEach { incoming ->
                        val existing = inventoryDao.getBySku(incoming.sku)
                        val entity = if (existing != null) {
                            incoming.copy(id = existing.id, updatedAt = now)
                        } else {
                            incoming.copy(updatedAt = now)
                        }
                        val savedId = inventoryDao.insertItem(entity)

                        val catNames = incoming.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val catIds = catNames.map { catName ->
                            categoryDao.getByName(catName)?.id ?: categoryDao.insertCategory(
                                CategoryEntity(name = catName, createdAt = now)
                            )
                        }
                        categoryDao.deleteCrossRefsForItem(savedId)
                        if (catIds.isNotEmpty()) {
                            categoryDao.insertCrossRefs(catIds.map { InventoryCategoryCrossRef(savedId, it) })
                        }

                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                inventoryItemId = savedId,
                                itemName = entity.name,
                                delta = entity.stockQty - (existing?.stockQty ?: 0.0),
                                previousQty = existing?.stockQty ?: 0.0,
                                newQty = entity.stockQty,
                                reason = "Impor CSV",
                                actorName = actorName,
                                createdAt = now
                            )
                        )
                    }
                }
                checkAndNotifyLowStock()
                triggerAutoBackupIfEnabled()
            }
            parsed
        }
}
