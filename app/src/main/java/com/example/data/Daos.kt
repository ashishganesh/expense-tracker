package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp ASC")
    suspend fun getTransactionsForAccountSync(accountId: Long): List<Transaction>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccount(accountId: Long)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE accountId = :accountId")
    fun getRecurringForAccount(accountId: Long): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurringSync(): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransaction): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringTransaction)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransaction)
}

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets WHERE accountId = :accountId")
    fun getBudgetsForAccount(accountId: Long): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget): Long

    @Query("DELETE FROM category_budgets WHERE accountId = :accountId AND category = :category")
    suspend fun deleteBudgetForCategory(accountId: Long, category: String)

    @Delete
    suspend fun deleteBudget(budget: CategoryBudget)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE accountId = :accountId")
    fun getGoalsForAccount(accountId: Long): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)
}

@Dao
interface BillReminderDao {
    @Query("SELECT * FROM bill_reminders WHERE accountId = :accountId ORDER BY dueDate ASC")
    fun getRemindersForAccount(accountId: Long): Flow<List<BillReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: BillReminder): Long

    @Update
    suspend fun updateReminder(reminder: BillReminder)

    @Delete
    suspend fun deleteReminder(reminder: BillReminder)
}

@Dao
interface CustomCategoryDao {
    @Query("SELECT * FROM custom_categories WHERE accountId = :accountId")
    fun getCustomCategoriesForAccount(accountId: Long): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: CustomCategory): Long

    @Delete
    suspend fun deleteCustomCategory(category: CustomCategory)
}
