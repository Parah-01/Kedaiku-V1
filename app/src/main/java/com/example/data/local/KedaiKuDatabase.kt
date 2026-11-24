package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StaffAccountEntity::class,
        CategoryEntity::class,
        CustomizationOptionEntity::class,
        ExpenseEntity::class,
        InventoryItemEntity::class,
        InventoryCategoryCrossRef::class,
        OrderEntity::class,
        OrderItemEntity::class,
        StockMovementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KedaiKuDatabase : RoomDatabase() {
    abstract fun staffDao(): StaffDao
    abstract fun categoryDao(): CategoryDao
    abstract fun customizationOptionDao(): CustomizationOptionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun orderDao(): OrderDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        @Volatile
        private var INSTANCE: KedaiKuDatabase? = null

        fun getInstance(context: Context): KedaiKuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KedaiKuDatabase::class.java,
                    "kedaiku_pos_offline.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
