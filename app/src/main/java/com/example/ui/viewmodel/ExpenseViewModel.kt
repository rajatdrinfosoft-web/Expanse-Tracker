package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.ExpenseRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL("All Time")
}

data class CategorySpending(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float
)

data class DailySpendingTrend(
    val dayLabel: String,
    val dateMillis: Long,
    val amount: Double,
    val isToday: Boolean
)

data class CategoryBudgetWarning(
    val category: ExpenseCategory,
    val spent: Double,
    val limit: Double,
    val percentUsed: Float,
    val isExceeded: Boolean
)

data class DashboardMetrics(
    val totalSpentThisMonth: Double = 0.0,
    val todaySpend: Double = 0.0,
    val remainingMonthlyBudget: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetPercentUsed: Float = 0f,
    val currencySymbol: String = "₹",
    val warnings: List<CategoryBudgetWarning> = emptyList()
)

data class AnalyticsState(
    val selectedDateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val totalFilteredSpend: Double = 0.0,
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val weeklyTrends: List<DailySpendingTrend> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList()
)

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.categoryBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity?> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recurringBills: StateFlow<List<com.example.data.model.RecurringBillEntity>> = repository.allRecurringBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PDF Export Dialog State
    val isPdfExportDialogOpen = MutableStateFlow(false)

    // Analytics filter state
    val selectedDateFilter = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")

    // Add Expense Sheet State
    val isAddExpenseSheetOpen = MutableStateFlow(false)
    val editingExpense = MutableStateFlow<ExpenseEntity?>(null)

    init {
        viewModelScope.launch {
            repository.removeDemoExpenses()
            repository.userSettings.firstOrNull()?.let { current ->
                if (current.currencySymbol == "$" || current.monthlyBudget == 2500.0) {
                    repository.updateUserSettings(
                        current.copy(
                            currencySymbol = if (current.currencySymbol == "$") "₹" else current.currencySymbol,
                            monthlyBudget = if (current.monthlyBudget == 2500.0) 0.0 else current.monthlyBudget
                        )
                    )
                }
            }
        }
    }

    // Combined Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allExpenses,
        categoryBudgets,
        userSettings
    ) { expenses, budgets, settings ->
        val currency = settings?.currencySymbol ?: "₹"
        val monthlyBudget = settings?.monthlyBudget ?: 0.0

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        var thisMonthSpend = 0.0
        var todaySpend = 0.0
        val categorySpendThisMonth = mutableMapOf<String, Double>()

        for (expense in expenses) {
            if (expense.dateMillis in startOfMonth..endOfMonth) {
                thisMonthSpend += expense.amount
                val currentCatTotal = categorySpendThisMonth[expense.category] ?: 0.0
                categorySpendThisMonth[expense.category] = currentCatTotal + expense.amount
            }
            if (expense.dateMillis in startOfToday..endOfToday) {
                todaySpend += expense.amount
            }
        }

        val remainingBudget = monthlyBudget - thisMonthSpend
        val daysElapsed = DateUtils.getDaysElapsedInMonth()
        val dailyAvg = if (daysElapsed > 0) thisMonthSpend / daysElapsed else 0.0
        val percentUsed = if (monthlyBudget > 0) ((thisMonthSpend / monthlyBudget) * 100).toFloat() else 0f

        // Check category budget warnings (>= 80% or > 100%)
        val budgetMap = budgets.associate { it.categoryName to it.monthlyLimit }
        val warnings = mutableListOf<CategoryBudgetWarning>()

        for ((catName, spent) in categorySpendThisMonth) {
            val limit = budgetMap[catName] ?: ExpenseCategory.fromString(catName).defaultLimit
            if (limit > 0) {
                val catPercent = ((spent / limit) * 100).toFloat()
                if (catPercent >= 80f) {
                    warnings.add(
                        CategoryBudgetWarning(
                            category = ExpenseCategory.fromString(catName),
                            spent = spent,
                            limit = limit,
                            percentUsed = catPercent,
                            isExceeded = catPercent >= 100f
                        )
                    )
                }
            }
        }

        DashboardMetrics(
            totalSpentThisMonth = thisMonthSpend,
            todaySpend = todaySpend,
            remainingMonthlyBudget = remainingBudget,
            dailyAverage = dailyAvg,
            monthlyBudget = monthlyBudget,
            budgetPercentUsed = percentUsed,
            currencySymbol = currency,
            warnings = warnings.sortedByDescending { it.percentUsed }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardMetrics()
    )

    // Combined Analytics State
    val analyticsState: StateFlow<AnalyticsState> = combine(
        allExpenses,
        selectedDateFilter,
        selectedCategoryFilter,
        searchQuery
    ) { expenses, dateFilter, catFilter, query ->
        val now = System.currentTimeMillis()
        val (startTime, endTime) = when (dateFilter) {
            DateRangeFilter.TODAY -> DateUtils.getStartOfDay(now) to DateUtils.getEndOfDay(now)
            DateRangeFilter.THIS_WEEK -> DateUtils.getStartOfWeek(now) to DateUtils.getEndOfDay(now)
            DateRangeFilter.THIS_MONTH -> DateUtils.getStartOfMonth(now) to DateUtils.getEndOfMonth(now)
            DateRangeFilter.ALL -> 0L to Long.MAX_VALUE
        }

        // Filter expenses
        val filtered = expenses.filter { expense ->
            val matchesDate = expense.dateMillis in startTime..endTime
            val matchesCategory = catFilter == null || expense.category.equals(catFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    expense.notes.contains(query, ignoreCase = true) ||
                    expense.category.contains(query, ignoreCase = true) ||
                    expense.paymentMethod.contains(query, ignoreCase = true) ||
                    expense.amount.toString().contains(query)
            matchesDate && matchesCategory && matchesQuery
        }

        val totalSpend = filtered.sumOf { it.amount }

        // Category breakdown
        val catSpendMap = mutableMapOf<ExpenseCategory, Double>()
        for (item in filtered) {
            val cat = ExpenseCategory.fromString(item.category)
            catSpendMap[cat] = (catSpendMap[cat] ?: 0.0) + item.amount
        }
        val breakdown = catSpendMap.map { (cat, amount) ->
            val pct = if (totalSpend > 0) ((amount / totalSpend) * 100).toFloat() else 0f
            CategorySpending(category = cat, totalAmount = amount, percentage = pct)
        }.sortedByDescending { it.totalAmount }

        // Weekly trends (Last 7 days daily totals)
        val dailyTrends = mutableListOf<DailySpendingTrend>()
        val cal = Calendar.getInstance()
        val todayStart = DateUtils.getStartOfDay(now)

        for (i in 6 downTo 0) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = DateUtils.getStartOfDay(cal.timeInMillis)
            val dayEnd = DateUtils.getEndOfDay(cal.timeInMillis)
            val dayLabel = DateUtils.getDayLabel(dayStart)

            val daySpend = expenses.filter { it.dateMillis in dayStart..dayEnd }.sumOf { it.amount }
            dailyTrends.add(
                DailySpendingTrend(
                    dayLabel = dayLabel,
                    dateMillis = dayStart,
                    amount = daySpend,
                    isToday = dayStart == todayStart
                )
            )
        }

        AnalyticsState(
            selectedDateFilter = dateFilter,
            selectedCategory = catFilter,
            searchQuery = query,
            totalFilteredSpend = totalSpend,
            categoryBreakdown = breakdown,
            weeklyTrends = dailyTrends,
            filteredExpenses = filtered
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsState()
    )

    fun openAddExpense(expenseToEdit: ExpenseEntity? = null) {
        editingExpense.value = expenseToEdit
        isAddExpenseSheetOpen.value = true
    }

    fun closeAddExpense() {
        editingExpense.value = null
        isAddExpenseSheetOpen.value = false
    }

    fun saveExpense(
        amount: Double,
        category: String,
        paymentMethod: String,
        dateMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val currentEdit = editingExpense.value
            if (currentEdit != null) {
                repository.updateExpense(
                    currentEdit.copy(
                        amount = amount,
                        category = category,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        notes = notes.trim()
                    )
                )
            } else {
                repository.insertExpense(
                    ExpenseEntity(
                        amount = amount,
                        category = category,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        notes = notes.trim()
                    )
                )
            }
            closeAddExpense()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun setDateFilter(filter: DateRangeFilter) {
        selectedDateFilter.value = filter
    }

    fun setCategoryFilter(category: String?) {
        selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        this.searchQuery.value = query
    }

    fun updateMonthlyBudget(newBudget: Double) {
        viewModelScope.launch {
            val current = userSettings.value ?: UserSettingsEntity()
            repository.updateUserSettings(current.copy(monthlyBudget = newBudget))
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            val current = userSettings.value ?: UserSettingsEntity()
            repository.updateUserSettings(current.copy(currencySymbol = symbol))
        }
    }

    fun updateCategoryBudget(categoryName: String, limit: Double) {
        viewModelScope.launch {
            repository.updateCategoryBudget(categoryName, limit)
        }
    }

    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.deleteAllExpenses()
        }
    }

    // PDF Export Dialog Controls
    fun openPdfExportDialog() {
        isPdfExportDialogOpen.value = true
    }

    fun closePdfExportDialog() {
        isPdfExportDialogOpen.value = false
    }

    // Recurring Bills Controls
    fun saveRecurringBill(
        id: Long = 0,
        title: String,
        amount: Double,
        category: String,
        paymentMethod: String = "UPI",
        billingFrequency: String = "Monthly",
        dueDayOfMonth: Int = 1,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val bill = com.example.data.model.RecurringBillEntity(
                id = id,
                title = title.trim(),
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                billingFrequency = billingFrequency,
                dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                notes = notes.trim()
            )
            if (id > 0) {
                repository.updateRecurringBill(bill)
            } else {
                repository.insertRecurringBill(bill)
            }
        }
    }

    fun deleteRecurringBill(bill: com.example.data.model.RecurringBillEntity) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
        }
    }

    fun markBillAsPaid(bill: com.example.data.model.RecurringBillEntity) {
        viewModelScope.launch {
            repository.logBillAsPaid(bill)
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
