package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String = "₹",
    val pin: String? = null,
    val carryForwardEnabled: Boolean = true,
    val monthlyBudget: Double = 0.0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val title: String,
    val timestamp: Long, // Epoc millis
    val remarks: String = "",
    val category: String,
    val isRecurring: Boolean = false
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val title: String,
    val category: String,
    val remarks: String = "",
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: Long,
    val lastProcessedDate: Long = 0L
)

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val category: String,
    val amount: Double
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long
)

@Entity(tableName = "bill_reminders")
data class BillReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val recurringFrequency: String? = null // null / "MONTHLY" / "WEEKLY"
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String
)
