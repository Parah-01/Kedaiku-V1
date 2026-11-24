package com.example.ui

import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.MenuRecipeEntity
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.local.StaffAccountEntity
import com.example.data.local.StaffRole
import com.example.data.local.StockMovementEntity
import com.example.data.prefs.StoreProfile
import com.example.ui.theme.ThemeMode
import com.example.util.LocalBackupInfo
import com.squareup.moshi.JsonClass

enum class AnalyticsPeriod(val label: String) {
    TODAY("Hari Ini"),
    WEEK("7 Hari"),
    MONTH("30 Hari")
}

@JsonClass(generateAdapter = true)
data class CartLineItem(
    val lineKey: String,
    val itemId: Long,
    val itemName: String,
    val basePrice: Long,
    val unitPrice: Long,
    val quantity: Int,
    val optionsSummary: String = ""
) {
    val lineTotal: Long get() = unitPrice * quantity
}

data class DraftEditContext(
    val orderId: Long,
    val orderNumber: String,
    val customerName: String = "",
    val orderType: OrderType = OrderType.TAKE_AWAY,
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val discountAmount: Long = 0L,
    val notes: String = ""
)

data class DailySalesPoint(
    val label: String,
    val revenue: Long,
    val orderCount: Int
)

data class TopSellingProduct(
    val itemName: String,
    val qtySold: Int,
    val revenue: Long
)

data class CategorySalesBreakdown(
    val categoryName: String,
    val qtySold: Int,
    val revenue: Long,
    val percentage: Float
)

data class PaymentBreakdown(
    val method: PaymentMethod,
    val totalAmount: Long,
    val count: Int,
    val percentage: Float
)

data class ExpenseCategoryBreakdown(
    val category: ExpenseCategory,
    val totalAmount: Long,
    val count: Int,
    val percentage: Float
)

data class AnalyticsSnapshot(
    val totalRevenue: Long = 0L,
    val totalCost: Long = 0L,
    val grossProfit: Long = 0L,
    val totalExpenses: Long = 0L,
    val netProfit: Long = 0L,
    val totalOrders: Int = 0,
    val averageOrderValue: Long = 0L,
    val chartPoints: List<DailySalesPoint> = emptyList(),
    val paymentBreakdowns: List<PaymentBreakdown> = emptyList(),
    val categoryBreakdowns: List<CategorySalesBreakdown> = emptyList(),
    val expenseBreakdowns: List<ExpenseCategoryBreakdown> = emptyList(),
    val topProducts: List<TopSellingProduct> = emptyList()
)

data class MainUiState(
    val isLoading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val storeProfile: StoreProfile = StoreProfile(),
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupTime: Long = 0L,
    val localSnapshots: List<LocalBackupInfo> = emptyList(),
    val allStaff: List<StaffAccountEntity> = emptyList(),
    val activeStaff: List<StaffAccountEntity> = emptyList(),
    val currentUser: StaffAccountEntity? = null,
    val isOwner: Boolean = false,
    val hasAnyOwner: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val customizationOptions: List<CustomizationOptionEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val menuRecipes: List<MenuRecipeEntity> = emptyList(),
    val effectiveStockMap: Map<Long, Double> = emptyMap(),
    val inventoryWithCategories: List<InventoryItemWithCategories> = emptyList(),
    val availableMenu: List<InventoryItemWithCategories> = emptyList(),
    val lowStockItems: List<InventoryItemEntity> = emptyList(),
    val orders: List<OrderWithItems> = emptyList(),
    val recentMovements: List<StockMovementEntity> = emptyList(),
    val cartLines: List<CartLineItem> = emptyList(),
    val draftEditContext: DraftEditContext? = null,
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.TODAY,
    val analytics: AnalyticsSnapshot = AnalyticsSnapshot(),
    val latestReceiptOrder: OrderWithItems? = null,
    val loginError: String? = null
) {
    val activeRole: StaffRole?
        get() = currentUser?.let { roleStr ->
            StaffRole.entries.find { it.name == roleStr.role }
        }
}
