package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StaffAccountEntity::class,
        CategoryEntity::class,
        CustomizationOptionEntity::class,
        ExpenseEntity::class,
        InventoryItemEntity::class,
        InventoryCategoryCrossRef::class,
        MenuRecipeEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        StockMovementEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KedaiKuDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun categoryDao(): CategoryDao
    abstract fun customizationOptionDao(): CustomizationOptionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun menuRecipeDao(): MenuRecipeDao
    abstract fun orderDao(): OrderDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: KedaiKuDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `menu_recipes` (
                        `menuItemId` INTEGER NOT NULL,
                        `ingredientItemId` INTEGER NOT NULL,
                        `quantityRequired` REAL NOT NULL,
                        PRIMARY KEY(`menuItemId`, `ingredientItemId`),
                        FOREIGN KEY(`menuItemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`ingredientItemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_recipes_menuItemId` ON `menu_recipes` (`menuItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_recipes_ingredientItemId` ON `menu_recipes` (`ingredientItemId`)")
            }
        }

        fun getInstance(context: Context): KedaiKuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KedaiKuDatabase::class.java,
                    "kedaiku_pos_offline.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
