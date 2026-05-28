package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.SyncSettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryBudget
import com.example.data.model.MonthlyBudget
import com.example.data.model.SyncSettings
import com.example.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        MonthlyBudget::class,
        CategoryBudget::class,
        SyncSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncSettingsDao(): SyncSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pulse_finance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
