package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryCategoryCrossRef
import com.example.data.local.InventoryItemEntity
import com.example.data.local.MenuRecipeEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.StockMovementEntity
import com.example.data.prefs.StoreProfile
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

@JsonClass(generateAdapter = true)
data class BackupCategoryDto(
    val id: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class BackupCategoryRefDto(
    val inventoryItemId: Long,
    val categoryId: Long
)

@JsonClass(generateAdapter = true)
data class BackupMenuRecipeDto(
    val menuItemId: Long,
    val ingredientItemId: Long,
    val quantityRequired: Double
)

@JsonClass(generateAdapter = true)
data class BackupCustomizationOptionDto(
    val id: Long,
    val optionType: String,
    val label: String,
    val extraPrice: Long,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class BackupExpenseDto(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Long,
    val notes: String = "",
    val recordedBy: String,
    val expenseDate: Long,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupInventoryDto(
    val id: Long,
    val sku: String,
    val name: String,
    val category: String,
    val itemType: String,
    val customizationGroup: String = "NONE",
    val price: Long,
    val costPrice: Long,
    val stockQty: Double,
    val minThreshold: Double,
    val unit: String,
    val isAvailable: Boolean,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupOrderItemDto(
    val id: Long,
    val orderId: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val optionsSummary: String = "",
    val unitPrice: Long,
    val unitCost: Long,
    val quantity: Int,
    val lineTotal: Long
)

@JsonClass(generateAdapter = true)
data class BackupOrderDto(
    val id: Long,
    val orderNumber: String,
    val customerName: String,
    val orderType: String,
    val status: String,
    val paymentMethod: String,
    val subtotal: Long,
    val discountAmount: Long,
    val taxAmount: Long,
    val totalAmount: Long,
    val paidAmount: Long,
    val changeAmount: Long,
    val notes: String,
    val cashierId: Long,
    val cashierName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<BackupOrderItemDto>
)

@JsonClass(generateAdapter = true)
data class BackupStockMovementDto(
    val id: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val delta: Double,
    val previousQty: Double,
    val newQty: Double,
    val reason: String,
    val actorName: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class BackupStoreDto(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val receiptFooter: String,
    val taxTakeAwayPercent: Int = 10,
    val taxShopeePercent: Int = 10,
    val taxGrabPercent: Int = 10
)

@JsonClass(generateAdapter = true)
data class KedaiKuBackupPayload(
    val schemaVersion: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val storeProfile: BackupStoreDto,
    val categories: List<BackupCategoryDto> = emptyList(),
    val categoryRefs: List<BackupCategoryRefDto> = emptyList(),
    val menuRecipes: List<BackupMenuRecipeDto> = emptyList(),
    val customizationOptions: List<BackupCustomizationOptionDto> = emptyList(),
    val expenses: List<BackupExpenseDto> = emptyList(),
    val inventory: List<BackupInventoryDto>,
    val orders: List<BackupOrderDto>,
    val stockMovements: List<BackupStockMovementDto>
)

data class LocalBackupInfo(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val sizeBytes: Long
)

object BackupManager {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(KedaiKuBackupPayload::class.java).indent("  ")

    fun buildPayloadJson(
        store: StoreProfile,
        categories: List<CategoryEntity>,
        categoryRefs: List<InventoryCategoryCrossRef>,
        menuRecipes: List<MenuRecipeEntity>,
        customizationOptions: List<CustomizationOptionEntity>,
        expenses: List<ExpenseEntity>,
        inventory: List<InventoryItemEntity>,
        orders: List<OrderWithItems>,
        movements: List<StockMovementEntity>
    ): String {
        val payload = KedaiKuBackupPayload(
            createdAt = System.currentTimeMillis(),
            storeProfile = BackupStoreDto(
                storeName = store.storeName,
                storeAddress = store.storeAddress,
                storePhone = store.storePhone,
                receiptFooter = store.receiptFooter,
                taxTakeAwayPercent = store.taxTakeAwayPercent,
                taxShopeePercent = store.taxShopeePercent,
                taxGrabPercent = store.taxGrabPercent
            ),
            categories = categories.map {
                BackupCategoryDto(it.id, it.name, it.description, it.createdAt)
            },
            categoryRefs = categoryRefs.map {
                BackupCategoryRefDto(it.inventoryItemId, it.categoryId)
            },
            menuRecipes = menuRecipes.map {
                BackupMenuRecipeDto(it.menuItemId, it.ingredientItemId, it.quantityRequired)
            },
            customizationOptions = customizationOptions.map {
                BackupCustomizationOptionDto(it.id, it.optionType, it.label, it.extraPrice, it.sortOrder)
            },
            expenses = expenses.map {
                BackupExpenseDto(
                    id = it.id,
                    title = it.title,
                    category = it.category,
                    amount = it.amount,
                    notes = it.notes,
                    recordedBy = it.recordedBy,
                    expenseDate = it.expenseDate,
                    createdAt = it.createdAt
                )
            },
            inventory = inventory.map {
                BackupInventoryDto(
                    id = it.id,
                    sku = it.sku,
                    name = it.name,
                    category = it.category,
                    itemType = it.itemType,
                    customizationGroup = it.customizationGroup,
                    price = it.price,
                    costPrice = it.costPrice,
                    stockQty = it.stockQty,
                    minThreshold = it.minThreshold,
                    unit = it.unit,
                    isAvailable = it.isAvailable,
                    updatedAt = it.updatedAt
                )
            },
            orders = orders.map { owi ->
                val o = owi.order
                BackupOrderDto(
                    id = o.id,
                    orderNumber = o.orderNumber,
                    customerName = o.customerName,
                    orderType = o.orderType,
                    status = o.status,
                    paymentMethod = o.paymentMethod,
                    subtotal = o.subtotal,
                    discountAmount = o.discountAmount,
                    taxAmount = o.taxAmount,
                    totalAmount = o.totalAmount,
                    paidAmount = o.paidAmount,
                    changeAmount = o.changeAmount,
                    notes = o.notes,
                    cashierId = o.cashierId,
                    cashierName = o.cashierName,
                    createdAt = o.createdAt,
                    updatedAt = o.updatedAt,
                    items = owi.items.map { item ->
                        BackupOrderItemDto(
                            id = item.id,
                            orderId = item.orderId,
                            inventoryItemId = item.inventoryItemId,
                            itemName = item.itemName,
                            optionsSummary = item.optionsSummary,
                            unitPrice = item.unitPrice,
                            unitCost = item.unitCost,
                            quantity = item.quantity,
                            lineTotal = item.lineTotal
                        )
                    }
                )
            },
            stockMovements = movements.map {
                BackupStockMovementDto(
                    id = it.id,
                    inventoryItemId = it.inventoryItemId,
                    itemName = it.itemName,
                    delta = it.delta,
                    previousQty = it.previousQty,
                    newQty = it.newQty,
                    reason = it.reason,
                    actorName = it.actorName,
                    createdAt = it.createdAt
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun writeAutoBackupSnapshot(context: Context, json: String): Long {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val now = System.currentTimeMillis()
        File(dir, "kedaiku_autobackup_latest.json").writeText(json, Charsets.UTF_8)
        File(dir, "kedaiku_snapshot_${Formatters.formatFileTimestamp(now)}.json").writeText(json, Charsets.UTF_8)

        val snapshots = dir.listFiles { file ->
            file.name.startsWith("kedaiku_snapshot_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        snapshots.drop(5).forEach { it.delete() }
        return now
    }

    fun listLocalSnapshots(context: Context): List<LocalBackupInfo> {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                LocalBackupInfo(
                    fileName = it.name,
                    filePath = it.absolutePath,
                    timestamp = it.lastModified(),
                    sizeBytes = it.length()
                )
            }.orEmpty()
    }

    fun readSnapshotFile(filePath: String): KedaiKuBackupPayload? {
        val file = File(filePath)
        if (!file.exists()) return null
        return parseBackupJson(file.readText(Charsets.UTF_8))
    }

    fun exportToUri(context: Context, uri: Uri, json: String) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Gagal menulis file backup.")
    }

    fun readFromUri(context: Context, uri: Uri): KedaiKuBackupPayload {
        val raw = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("Gagal membaca file backup.")
        return parseBackupJson(raw) ?: error("Format file backup tidak valid.")
    }

    private fun parseBackupJson(raw: String): KedaiKuBackupPayload? {
        return try {
            adapter.fromJson(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun BackupCategoryDto.toEntity() = CategoryEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt
    )

    fun BackupCategoryRefDto.toEntity() = InventoryCategoryCrossRef(
        inventoryItemId = inventoryItemId,
        categoryId = categoryId
    )

    fun BackupMenuRecipeDto.toEntity() = MenuRecipeEntity(
        menuItemId = menuItemId,
        ingredientItemId = ingredientItemId,
        quantityRequired = quantityRequired
    )

    fun BackupCustomizationOptionDto.toEntity() = CustomizationOptionEntity(
        id = id,
        optionType = optionType,
        label = label,
        extraPrice = extraPrice,
        sortOrder = sortOrder
    )

    fun BackupExpenseDto.toEntity() = ExpenseEntity(
        id = id,
        title = title,
        category = category,
        amount = amount,
        notes = notes,
        recordedBy = recordedBy,
        expenseDate = expenseDate,
        createdAt = createdAt
    )

    fun BackupInventoryDto.toEntity() = InventoryItemEntity(
        id = id,
        sku = sku,
        name = name,
        category = category,
        itemType = itemType,
        customizationGroup = customizationGroup,
        price = price,
        costPrice = costPrice,
        stockQty = stockQty,
        minThreshold = minThreshold,
        unit = unit,
        isAvailable = isAvailable,
        updatedAt = updatedAt
    )

    fun BackupOrderDto.toEntity() = OrderEntity(
        id = id,
        orderNumber = orderNumber,
        customerName = customerName,
        orderType = orderType,
        status = status,
        paymentMethod = paymentMethod,
        subtotal = subtotal,
        discountAmount = discountAmount,
        taxAmount = taxAmount,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        changeAmount = changeAmount,
        notes = notes,
        cashierId = cashierId,
        cashierName = cashierName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun BackupOrderItemDto.toEntity() = OrderItemEntity(
        id = id,
        orderId = orderId,
        inventoryItemId = inventoryItemId,
        itemName = itemName,
        optionsSummary = optionsSummary,
        unitPrice = unitPrice,
        unitCost = unitCost,
        quantity = quantity,
        lineTotal = lineTotal
    )

    fun BackupStockMovementDto.toEntity() = StockMovementEntity(
        id = id,
        inventoryItemId = inventoryItemId,
        itemName = itemName,
        delta = delta,
        previousQty = previousQty,
        newQty = newQty,
        reason = reason,
        actorName = actorName,
        createdAt = createdAt
    )
}
