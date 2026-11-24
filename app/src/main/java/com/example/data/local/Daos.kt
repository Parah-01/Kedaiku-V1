package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_accounts ORDER BY role DESC, name ASC")
    fun observeAllStaff(): Flow<List<StaffAccountEntity>>

    @Query("SELECT * FROM staff_accounts WHERE isActive = 1 ORDER BY role DESC, name ASC")
    fun observeActiveStaff(): Flow<List<StaffAccountEntity>>

    @Query("SELECT * FROM staff_accounts WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: Long): StaffAccountEntity?

    @Query("SELECT COUNT(*) FROM staff_accounts")
    suspend fun countStaff(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffAccountEntity): Long

    @Update
    suspend fun updateStaff(staff: StaffAccountEntity)

    @Delete
    suspend fun deleteStaff(staff: StaffAccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?

    @Query("SELECT * FROM inventory_category_cross_ref")
    suspend fun getAllCrossRefsSnapshot(): List<InventoryCategoryCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<InventoryCategoryCrossRef>)

    @Query("DELETE FROM inventory_category_cross_ref WHERE inventoryItemId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Long)

    @Query("DELETE FROM inventory_category_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}

@Dao
interface CustomizationOptionDao {
    @Query("SELECT * FROM customization_options ORDER BY optionType ASC, sortOrder ASC, id ASC")
    fun observeAllOptions(): Flow<List<CustomizationOptionEntity>>

    @Query("SELECT * FROM customization_options ORDER BY optionType ASC, sortOrder ASC, id ASC")
    suspend fun getAllOptionsSnapshot(): List<CustomizationOptionEntity>

    @Query("SELECT COUNT(*) FROM customization_options")
    suspend fun countOptions(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: CustomizationOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<CustomizationOptionEntity>)

    @Update
    suspend fun updateOption(option: CustomizationOptionEntity)

    @Delete
    suspend fun deleteOption(option: CustomizationOptionEntity)

    @Query("DELETE FROM customization_options")
    suspend fun deleteAllOptions()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, createdAt DESC")
    fun observeAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC, createdAt DESC")
    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY itemType DESC, category ASC, name ASC")
    fun observeAllInventory(): Flow<List<InventoryItemEntity>>

    @Transaction
    @Query("SELECT * FROM inventory_items ORDER BY itemType DESC, category ASC, name ASC")
    fun observeAllInventoryWithCategories(): Flow<List<InventoryItemWithCategories>>

    @Transaction
    @Query("SELECT * FROM inventory_items ORDER BY itemType DESC, category ASC, name ASC")
    suspend fun getAllInventoryWithCategoriesSnapshot(): List<InventoryItemWithCategories>

    @Query("SELECT * FROM inventory_items WHERE stockQty <= minThreshold ORDER BY stockQty ASC, name ASC")
    fun observeLowStockItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items ORDER BY itemType DESC, category ASC, name ASC")
    suspend fun getAllInventorySnapshot(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): InventoryItemEntity?

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun countItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItemEntity>): List<Long>

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAll()
}

@Dao
interface MenuRecipeDao {
    @Query("SELECT * FROM menu_recipes ORDER BY menuItemId ASC, ingredientItemId ASC")
    fun observeAllRecipes(): Flow<List<MenuRecipeEntity>>

    @Query("SELECT * FROM menu_recipes ORDER BY menuItemId ASC, ingredientItemId ASC")
    suspend fun getAllRecipesSnapshot(): List<MenuRecipeEntity>

    @Query("SELECT * FROM menu_recipes WHERE menuItemId = :menuItemId")
    suspend fun getRecipesForMenu(menuItemId: Long): List<MenuRecipeEntity>

    @Query("SELECT * FROM menu_recipes WHERE ingredientItemId = :ingredientItemId")
    suspend fun getRecipesForIngredient(ingredientItemId: Long): List<MenuRecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<MenuRecipeEntity>)

    @Query("DELETE FROM menu_recipes WHERE menuItemId = :menuItemId")
    suspend fun deleteRecipesForMenu(menuItemId: Long)

    @Query("DELETE FROM menu_recipes WHERE ingredientItemId = :ingredientItemId")
    suspend fun deleteRecipesForIngredient(ingredientItemId: Long)

    @Query("DELETE FROM menu_recipes")
    suspend fun deleteAllRecipes()
}

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems?

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersWithItemsSnapshot(): List<OrderWithItems>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun countOrders(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Long)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: String, updatedAt: Long)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Long)

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT 200")
    fun observeRecentMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC")
    suspend fun getAllMovementsSnapshot(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()
}
