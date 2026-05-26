package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val db: AppDatabase) {

    // DAOs
    private val accountDao = db.accountDao()
    private val transactionDao = db.transactionDao()
    private val recurringTransactionDao = db.recurringTransactionDao()
    private val categoryBudgetDao = db.categoryBudgetDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val billReminderDao = db.billReminderDao()
    private val customCategoryDao = db.customCategoryDao()

    // Accounts
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account) {
        // Also delete transactions of this account
        transactionDao.deleteTransactionsByAccount(account.id)
        accountDao.deleteAccount(account)
    }

    // Transactions
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionsForAccountSync(accountId: Long): List<Transaction> =
        transactionDao.getTransactionsForAccountSync(accountId)

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    // Recurring Transactions
    fun getRecurringForAccount(accountId: Long): Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getRecurringForAccount(accountId)

    suspend fun getAllRecurringSync(): List<RecurringTransaction> =
        recurringTransactionDao.getAllRecurringSync()

    suspend fun insertRecurring(recurring: RecurringTransaction): Long =
        recurringTransactionDao.insertRecurring(recurring)

    suspend fun updateRecurring(recurring: RecurringTransaction) =
        recurringTransactionDao.updateRecurring(recurring)

    suspend fun deleteRecurring(recurring: RecurringTransaction) =
        recurringTransactionDao.deleteRecurring(recurring)

    // Category Budgets
    fun getBudgetsForAccount(accountId: Long): Flow<List<CategoryBudget>> =
        categoryBudgetDao.getBudgetsForAccount(accountId)

    suspend fun insertBudget(budget: CategoryBudget): Long =
        categoryBudgetDao.insertBudget(budget)

    suspend fun deleteBudgetForCategory(accountId: Long, category: String) =
        categoryBudgetDao.deleteBudgetForCategory(accountId, category)

    suspend fun deleteBudget(budget: CategoryBudget) =
        categoryBudgetDao.deleteBudget(budget)

    // Savings Goals
    fun getGoalsForAccount(accountId: Long): Flow<List<SavingsGoal>> =
        savingsGoalDao.getGoalsForAccount(accountId)

    suspend fun insertGoal(goal: SavingsGoal): Long =
        savingsGoalDao.insertGoal(goal)

    suspend fun updateGoal(goal: SavingsGoal) =
        savingsGoalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: SavingsGoal) =
        savingsGoalDao.deleteGoal(goal)

    // Bill Reminders
    fun getRemindersForAccount(accountId: Long): Flow<List<BillReminder>> =
        billReminderDao.getRemindersForAccount(accountId)

    suspend fun insertReminder(reminder: BillReminder): Long =
        billReminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: BillReminder) =
        billReminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: BillReminder) =
        billReminderDao.deleteReminder(reminder)

    // Custom Categories
    fun getCustomCategoriesForAccount(accountId: Long): Flow<List<CustomCategory>> =
        customCategoryDao.getCustomCategoriesForAccount(accountId)

    suspend fun insertCustomCategory(category: CustomCategory): Long =
        customCategoryDao.insertCustomCategory(category)

    suspend fun deleteCustomCategory(category: CustomCategory) =
        customCategoryDao.deleteCustomCategory(category)
}
