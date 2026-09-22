package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    val categoryBudgets: Flow<List<CategoryBudgetEntity>> = expenseDao.getAllCategoryBudgets()

    val userSettings: Flow<UserSettingsEntity?> = expenseDao.getUserSettings()

    val allRecurringBills: Flow<List<RecurringBillEntity>> = expenseDao.getAllRecurringBills()

    fun getExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesBetween(startMillis, endMillis)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun updateCategoryBudget(categoryName: String, monthlyLimit: Double) {
        expenseDao.insertOrUpdateCategoryBudget(CategoryBudgetEntity(categoryName, monthlyLimit))
    }

    suspend fun updateUserSettings(settings: UserSettingsEntity) {
        expenseDao.insertOrUpdateUserSettings(settings)
    }

    // Recurring Bills & Subscriptions
    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long {
        return expenseDao.insertRecurringBill(bill)
    }

    suspend fun updateRecurringBill(bill: RecurringBillEntity) {
        expenseDao.updateRecurringBill(bill)
    }

    suspend fun deleteRecurringBill(bill: RecurringBillEntity) {
        expenseDao.deleteRecurringBill(bill)
    }

    suspend fun deleteRecurringBillById(id: Long) {
        expenseDao.deleteRecurringBillById(id)
    }

    suspend fun logBillAsPaid(bill: RecurringBillEntity) {
        val now = System.currentTimeMillis()
        // 1. Insert into Expense ledger
        val expense = ExpenseEntity(
            amount = bill.amount,
            category = bill.category,
            paymentMethod = bill.paymentMethod,
            dateMillis = now,
            notes = "Recurring Bill: ${bill.title}"
        )
        expenseDao.insertExpense(expense)

        // 2. Update lastPaidDateMillis on the recurring bill
        expenseDao.markBillAsPaid(bill.id, now)
    }

    suspend fun removeDemoExpenses() {
        val demoNotes = listOf(
            "Organic Grocery Market", "Metro Rail Pass", "Morning Artisan Coffee",
            "Running shoes & socks", "High-speed Internet Bill", "Lunch with colleagues",
            "Cinema tickets", "Vitamin supplements", "Electricity & Power bill",
            "Cab ride to airport", "Weekly family supermarket", "Notebook and desk accessories"
        )
        expenseDao.deleteExpensesByNotes(demoNotes)
    }

    suspend fun deleteAllExpenses() {
        expenseDao.deleteAllExpenses()
    }
}
