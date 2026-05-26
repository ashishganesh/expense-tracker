package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class MonthBalance(
    val monthKey: String, // "2026-05"
    val monthName: String, // "May 2026"
    val openingBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val closingBalance: Double
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    // Global lists
    val accounts = MutableStateFlow<List<Account>>(emptyList())
    val activeAccountId = MutableStateFlow<Long?>(null)
    val activeAccount = MutableStateFlow<Account?>(null)

    // Flow for current active account items
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recurringTransactions = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    val categoryBudgets = MutableStateFlow<List<CategoryBudget>>(emptyList())
    val savingsGoals = MutableStateFlow<List<SavingsGoal>>(emptyList())
    val billReminders = MutableStateFlow<List<BillReminder>>(emptyList())
    val customCategories = MutableStateFlow<List<CustomCategory>>(emptyList())

    // Calculated balances per month (carry-forward history)
    val monthBalances = MutableStateFlow<List<MonthBalance>>(emptyList())

    // Active PIN unlock state
    val isAppLocked = MutableStateFlow(false)
    val lockedAccountForSwitch = MutableStateFlow<Account?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database)

        // Observe accounts globally
        viewModelScope.launch {
            repository.allAccounts.collect { accList ->
                accounts.value = accList

                // Seed default account if empty
                if (accList.isEmpty()) {
                    val defaultAccId = repository.insertAccount(
                        Account(name = "Personal", currency = "₹")
                    )
                    activeAccountId.value = defaultAccId
                } else if (activeAccountId.value == null) {
                    // Default to first account
                    activeAccountId.value = accList.first().id
                }
            }
        }

        // React to activeAccountId changes
        viewModelScope.launch {
            activeAccountId.collect { id ->
                if (id != null) {
                    val acc = repository.getAccountById(id)
                    activeAccount.value = acc

                    // Check if PIN lock is set on this account
                    if (acc?.pin != null) {
                        isAppLocked.value = true
                    } else {
                        isAppLocked.value = false
                    }

                    // Reload all sub-items
                    launch {
                        repository.getTransactionsForAccount(id).collect { txList ->
                            transactions.value = txList
                            recomputeBalances(txList, acc?.carryForwardEnabled ?: true)
                        }
                    }
                    launch {
                        repository.getRecurringForAccount(id).collect { recList ->
                            recurringTransactions.value = recList
                        }
                    }
                    launch {
                        repository.getBudgetsForAccount(id).collect { budList ->
                            categoryBudgets.value = budList
                        }
                    }
                    launch {
                        repository.getGoalsForAccount(id).collect { goalList ->
                            savingsGoals.value = goalList
                        }
                    }
                    launch {
                        repository.getRemindersForAccount(id).collect { remList ->
                            billReminders.value = remList
                        }
                    }
                    launch {
                        repository.getCustomCategoriesForAccount(id).collect { catList ->
                            customCategories.value = catList
                        }
                    }

                    // Trigger scheduler check for recurring items
                    launch {
                        processRecurringTransactions(id)
                    }
                }
            }
        }
    }

    fun unlockAccount(pin: String): Boolean {
        val acc = lockedAccountForSwitch.value ?: activeAccount.value
        return if (acc != null && acc.pin == pin) {
            if (lockedAccountForSwitch.value != null) {
                activeAccountId.value = lockedAccountForSwitch.value!!.id
                lockedAccountForSwitch.value = null
            }
            isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun switchAccount(account: Account) {
        if (account.pin != null) {
            lockedAccountForSwitch.value = account
            isAppLocked.value = true
        } else {
            activeAccountId.value = account.id
            isAppLocked.value = false
        }
    }

    // Account setup triggers
    fun addAccount(name: String, currency: String, pin: String?, carryForwardEnabled: Boolean, monthlyBudget: Double) {
        viewModelScope.launch {
            repository.insertAccount(
                Account(
                    name = name,
                    currency = currency,
                    pin = if (pin.isNullOrBlank()) null else pin,
                    carryForwardEnabled = carryForwardEnabled,
                    monthlyBudget = monthlyBudget
                )
            )
        }
    }

    fun updateActiveAccountConfig(name: String, currency: String, pin: String?, carryForwardEnabled: Boolean, monthlyBudget: Double) {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                name = name,
                currency = currency,
                pin = if (pin.isNullOrBlank()) null else pin,
                carryForwardEnabled = carryForwardEnabled,
                monthlyBudget = monthlyBudget
            )
            repository.updateAccount(updated)
            activeAccount.value = updated
            // Instantly recompute list when carryForward toggle shifts
            recomputeBalances(transactions.value, carryForwardEnabled)
        }
    }

    fun deleteActiveAccount() {
        val current = activeAccount.value ?: return
        viewModelScope.launch {
            repository.deleteAccount(current)
            activeAccountId.value = null // Will trigger default seed/selection
        }
    }

    // Transaction Management
    fun addTransaction(
        type: String, amount: Double, title: String, timestamp: Long, remarks: String, category: String, isRecurring: Boolean = false
    ) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    accountId = accId,
                    type = type,
                    amount = amount,
                    title = title,
                    timestamp = timestamp,
                    remarks = remarks,
                    category = category,
                    isRecurring = isRecurring
                )
            )
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Custom Category Management
    fun addCustomCategory(name: String) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertCustomCategory(CustomCategory(accountId = accId, name = name))
        }
    }

    fun deleteCustomCategory(customCategory: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCustomCategory(customCategory)
        }
    }

    // Budgets
    fun setCategoryBudget(category: String, amount: Double) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertBudget(CategoryBudget(accountId = accId, category = category, amount = amount))
        }
    }

    fun deleteCategoryBudget(category: String) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.deleteBudgetForCategory(accId, category)
        }
    }

    // Savings Goals
    fun addSavingsGoal(name: String, targetAmount: Double, currentAmount: Double, targetDate: Long) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertGoal(
                SavingsGoal(
                    accountId = accId,
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = targetDate
                )
            )
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // Bill Reminders
    fun addBillReminder(title: String, amount: Double, dueDate: Long, recurringFrequency: String? = null) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertReminder(
                BillReminder(
                    accountId = accId,
                    title = title,
                    amount = amount,
                    dueDate = dueDate,
                    isPaid = false,
                    recurringFrequency = recurringFrequency
                )
            )
        }
    }

    fun markReminderPaid(reminder: BillReminder, logExpense: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(isPaid = true)
            repository.updateReminder(updated)

            if (logExpense) {
                // Instantly generate expense transaction
                addTransaction(
                    type = "EXPENSE",
                    amount = reminder.amount,
                    title = "Bill Paid: ${reminder.title}",
                    timestamp = System.currentTimeMillis(),
                    remarks = "Auto-created from Bill Reminders",
                    category = "Bills"
                )
            }
        }
    }

    fun deleteReminder(reminder: BillReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Recurring Setup
    fun addRecurringTransaction(
        type: String, amount: Double, title: String, category: String, remarks: String, frequency: String, startDate: Long
    ) {
        val accId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertRecurring(
                RecurringTransaction(
                    accountId = accId,
                    type = type,
                    amount = amount,
                    title = title,
                    category = category,
                    remarks = remarks,
                    frequency = frequency,
                    nextDueDate = startDate
                )
            )
        }
    }

    fun deleteRecurringTransaction(rec: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurring(rec)
        }
    }

    // Scheduler background routine
    private suspend fun processRecurringTransactions(accountId: Long) {
        val now = System.currentTimeMillis()
        val recurringSchedules = repository.getAllRecurringSync().filter { it.accountId == accountId }
        var updatedCount = 0

        for (rec in recurringSchedules) {
            var currentNextDate = rec.nextDueDate
            if (currentNextDate <= now) {
                var lastProcessed = rec.lastProcessedDate
                var loopCount = 0 // Safety cap

                while (currentNextDate <= now && loopCount < 50) {
                    loopCount++
                    // Create transaction
                    repository.insertTransaction(
                        Transaction(
                            accountId = accountId,
                            type = rec.type,
                            amount = rec.amount,
                            title = rec.title,
                            timestamp = currentNextDate,
                            remarks = "Recurring: ${rec.remarks}",
                            category = rec.category,
                            isRecurring = true
                        )
                    )

                    lastProcessed = currentNextDate
                    currentNextDate = advanceDateByFrequency(currentNextDate, rec.frequency)
                }

                repository.updateRecurring(
                    rec.copy(
                        nextDueDate = currentNextDate,
                        lastProcessedDate = lastProcessed
                    )
                )
                updatedCount += loopCount
            }
        }

        if (updatedCount > 0) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    "Processed $updatedCount recurring transactions automatically!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun advanceDateByFrequency(current: Long, frequency: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = current
        when (frequency.uppercase()) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1) // Default monthly
        }
        return cal.timeInMillis
    }

    // Helper date formatting
    private fun getMonthKey(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date(timestamp))
    }

    private fun getMonthName(monthKey: String): String {
        return try {
            val sdfSource = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdfSource.parse(monthKey) ?: return monthKey
            val sdfDest = SimpleDateFormat("MMMM yyyy", Locale.US)
            sdfDest.format(date)
        } catch (e: Exception) {
            monthKey
        }
    }

    // Dynamic Cascade Calculations
    private fun recomputeBalances(transactions: List<Transaction>, carryForwardEnabled: Boolean) {
        if (transactions.isEmpty()) {
            val currentKey = getMonthKey(System.currentTimeMillis())
            monthBalances.value = listOf(
                MonthBalance(
                    monthKey = currentKey,
                    monthName = getMonthName(currentKey),
                    openingBalance = 0.0,
                    totalIncome = 0.0,
                    totalExpense = 0.0,
                    closingBalance = 0.0
                )
            )
            return
        }

        val sortedTx = transactions.sortedBy { it.timestamp }
        val earliestTime = sortedTx.first().timestamp
        val latestTime = maxOf(sortedTx.last().timestamp, System.currentTimeMillis())

        val cal = Calendar.getInstance()
        cal.timeInMillis = earliestTime
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val currentCal = Calendar.getInstance()
        currentCal.timeInMillis = latestTime
        currentCal.set(Calendar.DAY_OF_MONTH, 1)

        val list = mutableListOf<MonthBalance>()
        var runningCarryForward = 0.0

        val txGrouped = transactions.groupBy { getMonthKey(it.timestamp) }

        while (!cal.after(currentCal)) {
            val key = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
            val name = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)

            val monthTxs = txGrouped[key] ?: emptyList()
            val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val opening = if (carryForwardEnabled) runningCarryForward else 0.0
            val closing = opening + income - expense

            list.add(
                MonthBalance(
                    monthKey = key,
                    monthName = name,
                    openingBalance = opening,
                    totalIncome = income,
                    totalExpense = expense,
                    closingBalance = closing
                )
            )

            runningCarryForward = if (carryForwardEnabled) closing else 0.0
            cal.add(Calendar.MONTH, 1)
        }

        monthBalances.value = list
    }

    // Share utility helper
    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // BACKUP & RESTORE: EXPORT CSV
    fun exportTransactionsToCsv(context: Context) {
        val activeAcc = activeAccount.value ?: return
        val txs = transactions.value.sortedByDescending { it.timestamp }

        viewModelScope.launch(Dispatchers.IO) {
            val csvFile = File(context.cacheDir, "FinFlow_Backup_${activeAcc.name}.csv")
            try {
                FileOutputStream(csvFile).use { fos ->
                    val header = "Transaction ID,Type,Amount,Title,Date,Category,Remarks,Recurring\n"
                    fos.write(header.toByteArray())

                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    for (tx in txs) {
                        val dateStr = sdf.format(Date(tx.timestamp))
                        val cleanTitle = tx.title.replace("\"", "\"\"")
                        val cleanRemarks = tx.remarks.replace("\"", "\"\"")
                        val line = "${tx.id},${tx.type},${tx.amount},\"$cleanTitle\",$dateStr,${tx.category},\"$cleanRemarks\",${tx.isRecurring}\n"
                        fos.write(line.toByteArray())
                    }
                }

                withContext(Dispatchers.Main) {
                    shareFile(context, csvFile, "text/csv", "Export CSV Transactions")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // BACKUP & RESTORE: EXPORT JSON
    fun exportStateToJson(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val rootObj = JSONObject()

                // 1. Accounts
                val accountsArray = JSONArray()
                val liveAccounts = db.accountDao().getAllAccounts().first()
                for (acc in liveAccounts) {
                    val accObj = JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("currency", acc.currency)
                        put("pin", acc.pin ?: JSONObject.NULL)
                        put("carryForwardEnabled", acc.carryForwardEnabled)
                        put("monthlyBudget", acc.monthlyBudget)
                    }
                    accountsArray.put(accObj)
                }
                rootObj.put("accounts", accountsArray)

                // 2. Transactions
                val txArray = JSONArray()
                val liveTxs = db.transactionDao().getAllTransactions().first()
                for (tx in liveTxs) {
                    val txObj = JSONObject().apply {
                        put("accountId", tx.accountId)
                        put("type", tx.type)
                        put("amount", tx.amount)
                        put("title", tx.title)
                        put("timestamp", tx.timestamp)
                        put("remarks", tx.remarks)
                        put("category", tx.category)
                        put("isRecurring", tx.isRecurring)
                    }
                    txArray.put(txObj)
                }
                rootObj.put("transactions", txArray)

                // 3. Recurring
                val recArray = JSONArray()
                val liveRecs = db.recurringTransactionDao().getAllRecurringSync()
                for (rec in liveRecs) {
                    val recObj = JSONObject().apply {
                        put("accountId", rec.accountId)
                        put("type", rec.type)
                        put("amount", rec.amount)
                        put("title", rec.title)
                        put("category", rec.category)
                        put("remarks", rec.remarks)
                        put("frequency", rec.frequency)
                        put("nextDueDate", rec.nextDueDate)
                        put("lastProcessedDate", rec.lastProcessedDate)
                    }
                    recArray.put(recObj)
                }
                rootObj.put("recurring_transactions", recArray)

                // Write file
                val backupFile = File(context.cacheDir, "FinFlow_Store_Backup.json")
                FileOutputStream(backupFile).use { fos ->
                    fos.write(rootObj.toString(2).toByteArray())
                }

                withContext(Dispatchers.Main) {
                    shareFile(context, backupFile, "application/json", "Export JSON Backup")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export JSON: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // BACKUP & RESTORE: IMPORT JSON
    fun importStateFromJson(context: Context, jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            viewModelScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)

                // Wipe existing database first to prevent duplicate/stray accounts
                db.clearAllTables()

                // 1. Restore Accounts
                val accountsArray = root.optJSONArray("accounts")
                if (accountsArray != null) {
                    for (i in 0 until accountsArray.length()) {
                        val obj = accountsArray.getJSONObject(i)
                        val pinVal = if (obj.isNull("pin")) null else obj.getString("pin")
                        db.accountDao().insertAccount(
                            Account(
                                id = obj.optLong("id", 0L),
                                name = obj.getString("name"),
                                currency = obj.optString("currency", "₹"),
                                pin = pinVal,
                                carryForwardEnabled = obj.optBoolean("carryForwardEnabled", true),
                                monthlyBudget = obj.optDouble("monthlyBudget", 0.0)
                            )
                        )
                    }
                }

                // 2. Restore Transactions
                val txArray = root.optJSONArray("transactions")
                if (txArray != null) {
                    for (i in 0 until txArray.length()) {
                        val obj = txArray.getJSONObject(i)
                        db.transactionDao().insertTransaction(
                            Transaction(
                                accountId = obj.getLong("accountId"),
                                type = obj.getString("type"),
                                amount = obj.getDouble("amount"),
                                title = obj.getString("title"),
                                timestamp = obj.getLong("timestamp"),
                                remarks = obj.optString("remarks", ""),
                                category = obj.getString("category"),
                                isRecurring = obj.optBoolean("isRecurring", false)
                            )
                        )
                    }
                }

                // 3. Restore Recurring
                val recArray = root.optJSONArray("recurring_transactions")
                if (recArray != null) {
                    for (i in 0 until recArray.length()) {
                        val obj = recArray.getJSONObject(i)
                        db.recurringTransactionDao().insertRecurring(
                            RecurringTransaction(
                                accountId = obj.getLong("accountId"),
                                type = obj.getString("type"),
                                amount = obj.getDouble("amount"),
                                title = obj.getString("title"),
                                category = obj.getString("category"),
                                remarks = obj.optString("remarks", ""),
                                frequency = obj.getString("frequency"),
                                nextDueDate = obj.getLong("nextDueDate"),
                                lastProcessedDate = obj.optLong("lastProcessedDate", 0L)
                            )
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
                    // Force re-collect trigger in UI
                    val pastActive = activeAccountId.value
                    activeAccountId.value = null
                    activeAccountId.value = pastActive
                }
            }
            return true
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to parse backup metadata: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
