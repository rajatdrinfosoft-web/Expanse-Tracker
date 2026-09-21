package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    val categoryBudgets: Flow<List<CategoryBudgetEntity>> = expenseDao.getAllCategoryBudgets()

    val userSettings: Flow<UserSettingsEntity?> = expenseDao.getUserSettings()

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

    suspend fun populateSampleData() {
        com.example.data.db.ExpenseDatabase.populateInitialData(expenseDao)
    }

    suspend fun deleteAllExpenses() {
        expenseDao.deleteAllExpenses()
    }
}
