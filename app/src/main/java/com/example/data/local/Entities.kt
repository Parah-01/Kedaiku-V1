package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class StaffRole(val label: String) {
    OWNER("Owner"),
    KARYAWAN("Karyawan")
}

enum class InventoryType(val label: String) {
    MENU("Menu Jual"),
    INGREDIENT("Bahan Baku")
}

enum class CustomizationGroup(val label: String) {
    DRINK("Minuman (Varian & Toping)"),
    FOOD("Makanan (Level 1-5)"),
    NONE("Standar (Tanpa Opsi)")
}

enum class CustomizationOptionType(val label: String) {
    DRINK_VARIANT("Varian Minuman"),
    DRINK_TOPPING("Toping Minuman"),
    FOOD_LEVEL("Level Makanan (1-5)")
}

enum class OrderType(val label: String) {
    TAKE_AWAY("Take Away"),
    ONLINE_SHOPEE("Online • Shopee"),
    ONLINE_GRAB("Online • Grab")
}

enum class OrderStatus(val label: String) {
    DRAFT("Draf"),
    PENDING("Antrean"),
    PREPARING("Diproses"),
    READY("Siap Saji"),
    COMPLETED("Selesai"),
    CANCELLED("Dibatalkan")
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    QRIS("QRIS")
}

enum class ExpenseCategory(val label: String) {
    OPERATIONAL("Biaya Operasional"),
    SALARY("Gaji Karyawan"),
    INGREDIENT_PURCHASE("Belanja Bahan Baku")
}

@Entity(tableName = "staff_accounts")
data class StaffAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val username: String,
    val role: String,
    val pinSalt: String,
    val pinHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customization_options")
data class CustomizationOptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val optionType: String, // CustomizationOptionType.name
    val label: String,
    val extraPrice: Long,
    val sortOrder: Int = 0
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val category: String, // ExpenseCategory.name
    val amount: Long,
    val notes: String = "",
    val recordedBy: String,
    val expenseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sku: String,
    val name: String,
    val category: String,
    val itemType: String,
    val customizationGroup: String = CustomizationGroup.NONE.name,
    val price: Long,
    val costPrice: Long,
    val stockQty: Double,
    val minThreshold: Double,
    val unit: String,
    val isAvailable: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQty <= minThreshold && stockQty > 0.0

    val isOutOfStock: Boolean
        get() = stockQty <= 0.0
}

@Entity(
    tableName = "inventory_category_cross_ref",
    primaryKeys = ["inventoryItemId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["inventoryItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("inventoryItemId")]
)
data class InventoryCategoryCrossRef(
    val inventoryItemId: Long,
    val categoryId: Long
)

data class InventoryItemWithCategories(
    @Embedded val item: InventoryItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = InventoryCategoryCrossRef::class,
            parentColumn = "inventoryItemId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity>
) {
    val categoryNames: List<String>
        get() = if (categories.isNotEmpty()) {
            categories.map { it.name }
        } else {
            item.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: Long,
    val inventoryItemId: Long,
    val itemName: String,
    val optionsSummary: String = "",
    val unitPrice: Long,
    val unitCost: Long,
    val quantity: Int,
    val lineTotal: Long
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val inventoryItemId: Long,
    val itemName: String,
    val delta: Double,
    val previousQty: Double,
    val newQty: Double,
    val reason: String,
    val actorName: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)
