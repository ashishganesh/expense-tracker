package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Account::class,
        Transaction::class,
        RecurringTransaction::class,
        CategoryBudget::class,
        SavingsGoal::class,
        BillReminder::class,
        CustomCategory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun billReminderDao(): BillReminderDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finflow_database"
                )
                    .fallbackToDestructiveMigration() // Simple for offline-first development setup
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
