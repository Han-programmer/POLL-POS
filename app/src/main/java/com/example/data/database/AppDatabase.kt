package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        Product::class,
        Category::class,
        Customer::class,
        Transaction::class,
        TransactionItem::class,
        TransactionPayment::class,
        StockMovement::class,
        CashShift::class,
        CashEntry::class,
        StaffUser::class,
        StoreSettings::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockDao(): StockDao
    abstract fun cashShiftDao(): CashShiftDao
    abstract fun staffDao(): StaffDao
    abstract fun storeSettingsDao(): StoreSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poll_pos_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
