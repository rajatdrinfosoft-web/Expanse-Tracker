package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val dateMillis: Long,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val categoryName: String,
    val monthlyLimit: Double
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val monthlyBudget: Double = 0.0,
    val currencySymbol: String = "₹"
)

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String = "UPI",
    val billingFrequency: String = "Monthly", // "Monthly", "Yearly", "Weekly"
    val dueDayOfMonth: Int = 1,              // 1 to 31
    val notes: String = "",
    val isActive: Boolean = true,
    val lastPaidDateMillis: Long? = null
) {
    fun isDueSoon(): Boolean {
        val today = java.util.Calendar.getInstance()
        val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        val diff = dueDayOfMonth - currentDay
        return diff in 0..5
    }

    fun isOverdue(): Boolean {
        val today = java.util.Calendar.getInstance()
        val currentDay = today.get(java.util.Calendar.DAY_OF_MONTH)
        if (lastPaidDateMillis != null) {
            val lastPaidCal = java.util.Calendar.getInstance().apply { timeInMillis = lastPaidDateMillis }
            if (lastPaidCal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH) &&
                lastPaidCal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)
            ) {
                return false // Already paid for current month
            }
        }
        return currentDay > dueDayOfMonth
    }

    fun isPaidForCurrentMonth(): Boolean {
        if (lastPaidDateMillis == null) return false
        val today = java.util.Calendar.getInstance()
        val lastPaidCal = java.util.Calendar.getInstance().apply { timeInMillis = lastPaidDateMillis }
        return lastPaidCal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH) &&
                lastPaidCal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)
    }
}

