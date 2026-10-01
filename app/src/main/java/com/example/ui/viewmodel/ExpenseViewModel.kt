package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringBillEntity
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
    LAST_MONTH("Last Month"),
    SPECIFIC_MONTH("Select Month"),
    ALL("All Time"),
    CUSTOM("Custom")
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
    val isToday: Boolean = false,
    val isPeak: Boolean = false,
    val subtitle: String = ""
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
    val isBudgetSet: Boolean = false,
    val budgetPercentUsed: Float = 0f,
    val projectedMonthlySpend: Double = 0.0,
    val thisMonthExpenseCount: Int = 0,
    val todayExpenseCount: Int = 0,
    val daysElapsed: Int = 1,
    val daysInMonth: Int = 30,
    val topCategoryName: String? = null,
    val topCategoryAmount: Double = 0.0,
    val topCategoryPercent: Float = 0f,
    val currencySymbol: String = "₹",
    val warnings: List<CategoryBudgetWarning> = emptyList()
)

data class AnalyticsState(
    val selectedDateFilter: DateRangeFilter = DateRangeFilter.THIS_MONTH,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-indexed
    val customStartMillis: Long = 0L,
    val customEndMillis: Long = 0L,
    val periodTitle: String = "",
    val periodSubtitle: String = "",
    val totalFilteredSpend: Double = 0.0,
    val totalExpenseCount: Int = 0,
    val dailyAverageInPeriod: Double = 0.0,
    val daysInPeriod: Int = 1,
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val spendingTrends: List<DailySpendingTrend> = emptyList(),
    val trendChartTitle: String = "Spending Trend",
    val trendChartSubtitle: String = "",
    val filteredExpenses: List<ExpenseEntity> = emptyList()
)

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.categoryBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettingsEntity?> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recurringBills: StateFlow<List<RecurringBillEntity>> = repository.allRecurringBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PDF Export Dialog State
    val isPdfExportDialogOpen = MutableStateFlow(false)

    // Analytics filter state
    val selectedDateFilter = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")

    // Specific Month and Year navigation for Analytics
    val selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0 to 11

    // Custom date range state
    val customRangeStart = MutableStateFlow(DateUtils.getStartOfMonth())
    val customRangeEnd = MutableStateFlow(DateUtils.getEndOfMonth())

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

    // Combined Dashboard Metrics - Fully functional whether monthly budget is set or not
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allExpenses,
        categoryBudgets,
        userSettings
    ) { expenses, budgets, settings ->
        val currency = settings?.currencySymbol ?: "₹"
        val monthlyBudget = settings?.monthlyBudget ?: 0.0
        val isBudgetSet = monthlyBudget > 0.0

        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)
        val daysInMonth = DateUtils.getDaysInMonth(currentYear, currentMonth)
        val daysElapsed = DateUtils.getDaysElapsedInMonth()

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val startOfToday = DateUtils.getStartOfDay()
        val endOfToday = DateUtils.getEndOfDay()

        var thisMonthSpend = 0.0
        var todaySpend = 0.0
        var thisMonthCount = 0
        var todayCount = 0
        val categorySpendThisMonth = mutableMapOf<String, Double>()

        for (expense in expenses) {
            if (expense.dateMillis in startOfMonth..endOfMonth) {
                thisMonthSpend += expense.amount
                thisMonthCount++
                val currentCatTotal = categorySpendThisMonth[expense.category] ?: 0.0
                categorySpendThisMonth[expense.category] = currentCatTotal + expense.amount
            }
            if (expense.dateMillis in startOfToday..endOfToday) {
                todaySpend += expense.amount
                todayCount++
            }
        }

        val remainingBudget = if (isBudgetSet) (monthlyBudget - thisMonthSpend) else 0.0
        val dailyAvg = if (daysElapsed > 0) thisMonthSpend / daysElapsed else 0.0
        val projectedMonthly = dailyAvg * daysInMonth
        val percentUsed = if (isBudgetSet) ((thisMonthSpend / monthlyBudget) * 100).toFloat() else 0f

        // Top Category
        val topCategoryEntry = categorySpendThisMonth.maxByOrNull { it.value }
        val topCategoryName = topCategoryEntry?.key
        val topCategoryAmount = topCategoryEntry?.value ?: 0.0
        val topCategoryPercent = if (thisMonthSpend > 0) ((topCategoryAmount / thisMonthSpend) * 100).toFloat() else 0f

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
            isBudgetSet = isBudgetSet,
            budgetPercentUsed = percentUsed,
            projectedMonthlySpend = projectedMonthly,
            thisMonthExpenseCount = thisMonthCount,
            todayExpenseCount = todayCount,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            topCategoryPercent = topCategoryPercent,
            currencySymbol = currency,
            warnings = warnings.sortedByDescending { it.percentUsed }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardMetrics()
    )

    // Combined Analytics State with support for Previous Month, Specific Month, and date filters
    val analyticsState: StateFlow<AnalyticsState> = combine(
        allExpenses,
        selectedDateFilter,
        selectedCategoryFilter,
        searchQuery,
        selectedYear,
        selectedMonth,
        customRangeStart,
        customRangeEnd
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val expenses = args[0] as List<ExpenseEntity>
        val dateFilter = args[1] as DateRangeFilter
        val catFilter = args[2] as String?
        val query = args[3] as String
        val year = args[4] as Int
        val month = args[5] as Int
        val customStart = args[6] as Long
        val customEnd = args[7] as Long

        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance()
        val currentYear = calNow.get(Calendar.YEAR)
        val currentMonth = calNow.get(Calendar.MONTH)

        // Calculate time boundary & labels based on date filter
        val (startTime, endTime, periodTitle, periodSubtitle, daysInPeriod) = when (dateFilter) {
            DateRangeFilter.TODAY -> {
                val s = DateUtils.getStartOfDay(now)
                val e = DateUtils.getEndOfDay(now)
                Tuple5(s, e, "Today", DateUtils.formatShortDate(now), 1)
            }
            DateRangeFilter.THIS_WEEK -> {
                val s = DateUtils.getStartOfWeek(now)
                val e = DateUtils.getEndOfWeek(now)
                Tuple5(s, e, "This Week", "${DateUtils.formatShortDate(s)} – ${DateUtils.formatShortDate(e)}", 7)
            }
            DateRangeFilter.THIS_MONTH -> {
                val s = DateUtils.getStartOfMonth(currentYear, currentMonth)
                val e = DateUtils.getEndOfMonth(currentYear, currentMonth)
                val days = DateUtils.getDaysInMonth(currentYear, currentMonth)
                Tuple5(s, e, DateUtils.formatMonthYear(currentYear, currentMonth), "Current month to date", days)
            }
            DateRangeFilter.LAST_MONTH -> {
                val calPrev = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val prevYear = calPrev.get(Calendar.YEAR)
                val prevMonth = calPrev.get(Calendar.MONTH)
                val s = DateUtils.getStartOfMonth(prevYear, prevMonth)
                val e = DateUtils.getEndOfMonth(prevYear, prevMonth)
                val days = DateUtils.getDaysInMonth(prevYear, prevMonth)
                Tuple5(s, e, DateUtils.formatMonthYear(prevYear, prevMonth), "Previous Month Summary", days)
            }
            DateRangeFilter.SPECIFIC_MONTH -> {
                val s = DateUtils.getStartOfMonth(year, month)
                val e = DateUtils.getEndOfMonth(year, month)
                val days = DateUtils.getDaysInMonth(year, month)
                val isCurrent = year == currentYear && month == currentMonth
                val subtitle = if (isCurrent) "Current Month" else "Past Month Archive"
                Tuple5(s, e, DateUtils.formatMonthYear(year, month), subtitle, days)
            }
            DateRangeFilter.ALL -> {
                Tuple5(0L, Long.MAX_VALUE, "All Time", "All recorded expenses", 365)
            }
            DateRangeFilter.CUSTOM -> {
                val days = (((customEnd - customStart) / 86400000L) + 1).toInt().coerceAtLeast(1)
                Tuple5(customStart, customEnd, "Custom Range", "${DateUtils.formatShortDate(customStart)} – ${DateUtils.formatShortDate(customEnd)}", days)
            }
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
        val dailyAverage = if (daysInPeriod > 0) totalSpend / daysInPeriod else 0.0

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

        // Spending trend generation
        val (trendData, chartTitle, chartSubtitle) = when (dateFilter) {
            DateRangeFilter.THIS_MONTH, DateRangeFilter.LAST_MONTH, DateRangeFilter.SPECIFIC_MONTH -> {
                // Monthly breakdown by weeks of that month (Week 1, Week 2, Week 3, Week 4, Week 5)
                val targetYear = if (dateFilter == DateRangeFilter.THIS_MONTH) currentYear
                else if (dateFilter == DateRangeFilter.LAST_MONTH) {
                    val p = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    p.get(Calendar.YEAR)
                } else year

                val targetMonth = if (dateFilter == DateRangeFilter.THIS_MONTH) currentMonth
                else if (dateFilter == DateRangeFilter.LAST_MONTH) {
                    val p = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    p.get(Calendar.MONTH)
                } else month

                val totalDays = DateUtils.getDaysInMonth(targetYear, targetMonth)
                val trends = mutableListOf<DailySpendingTrend>()

                val weekBuckets = listOf(
                    1 to 7,
                    8 to 14,
                    15 to 21,
                    22 to 28,
                    29 to totalDays
                )

                for ((idx, bucket) in weekBuckets.withIndex()) {
                    if (bucket.first > totalDays) continue
                    val startDay = bucket.first
                    val endDay = bucket.second.coerceAtMost(totalDays)

                    val startCal = Calendar.getInstance().apply {
                        set(targetYear, targetMonth, startDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = Calendar.getInstance().apply {
                        set(targetYear, targetMonth, endDay, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    val weekSpend = expenses.filter { it.dateMillis in startCal.timeInMillis..endCal.timeInMillis }.sumOf { it.amount }
                    val label = "W${idx + 1} (${startDay}–${endDay})"
                    trends.add(
                        DailySpendingTrend(
                            dayLabel = label,
                            dateMillis = startCal.timeInMillis,
                            amount = weekSpend,
                            isToday = now in startCal.timeInMillis..endCal.timeInMillis,
                            subtitle = "${DateUtils.formatMonthShort(targetMonth)} $startDay–$endDay"
                        )
                    )
                }
                val maxSpend = trends.maxOfOrNull { it.amount } ?: 0.0
                val markedTrends = trends.map { it.copy(isPeak = it.amount > 0 && it.amount == maxSpend) }
                Triple(markedTrends, "Weekly Breakdown ($periodTitle)", "Total spend across each week of the month")
            }
            DateRangeFilter.THIS_WEEK -> {
                // 7 days of this week (Mon to Sun)
                val trends = mutableListOf<DailySpendingTrend>()
                val weekStart = DateUtils.getStartOfWeek(now)
                val cal = Calendar.getInstance()
                for (i in 0..6) {
                    cal.timeInMillis = weekStart
                    cal.add(Calendar.DAY_OF_YEAR, i)
                    val dayStart = DateUtils.getStartOfDay(cal.timeInMillis)
                    val dayEnd = DateUtils.getEndOfDay(cal.timeInMillis)
                    val spend = expenses.filter { it.dateMillis in dayStart..dayEnd }.sumOf { it.amount }
                    trends.add(
                        DailySpendingTrend(
                            dayLabel = DateUtils.getDayLabel(dayStart),
                            dateMillis = dayStart,
                            amount = spend,
                            isToday = dayStart == DateUtils.getStartOfDay(now),
                            subtitle = DateUtils.formatShortDate(dayStart)
                        )
                    )
                }
                val maxSpend = trends.maxOfOrNull { it.amount } ?: 0.0
                val markedTrends = trends.map { it.copy(isPeak = it.amount > 0 && it.amount == maxSpend) }
                Triple(markedTrends, "Daily Spending Trend", "Monday to Sunday spending")
            }
            DateRangeFilter.TODAY -> {
                // Today vs Yesterday & 3-day view
                val trends = mutableListOf<DailySpendingTrend>()
                val cal = Calendar.getInstance()
                val todayStart = DateUtils.getStartOfDay(now)
                for (i in 4 downTo 0) {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -i)
                    val dayStart = DateUtils.getStartOfDay(cal.timeInMillis)
                    val dayEnd = DateUtils.getEndOfDay(cal.timeInMillis)
                    val spend = expenses.filter { it.dateMillis in dayStart..dayEnd }.sumOf { it.amount }
                    trends.add(
                        DailySpendingTrend(
                            dayLabel = if (dayStart == todayStart) "Today" else DateUtils.getDayLabel(dayStart),
                            dateMillis = dayStart,
                            amount = spend,
                            isToday = dayStart == todayStart,
                            subtitle = DateUtils.formatShortDate(dayStart)
                        )
                    )
                }
                val maxSpend = trends.maxOfOrNull { it.amount } ?: 0.0
                val markedTrends = trends.map { it.copy(isPeak = it.amount > 0 && it.amount == maxSpend) }
                Triple(markedTrends, "Recent Days Trend", "Daily comparison leading up to today")
            }
            DateRangeFilter.ALL, DateRangeFilter.CUSTOM -> {
                // Last 6 months or multi-period trend
                val trends = mutableListOf<DailySpendingTrend>()
                val cal = Calendar.getInstance()
                for (i in 5 downTo 0) {
                    cal.timeInMillis = now
                    cal.add(Calendar.MONTH, -i)
                    val y = cal.get(Calendar.YEAR)
                    val m = cal.get(Calendar.MONTH)
                    val mStart = DateUtils.getStartOfMonth(y, m)
                    val mEnd = DateUtils.getEndOfMonth(y, m)
                    val spend = expenses.filter { it.dateMillis in mStart..mEnd }.sumOf { it.amount }
                    trends.add(
                        DailySpendingTrend(
                            dayLabel = DateUtils.formatMonthShort(m),
                            dateMillis = mStart,
                            amount = spend,
                            isToday = y == currentYear && m == currentMonth,
                            subtitle = "$y"
                        )
                    )
                }
                val maxSpend = trends.maxOfOrNull { it.amount } ?: 0.0
                val markedTrends = trends.map { it.copy(isPeak = it.amount > 0 && it.amount == maxSpend) }
                Triple(markedTrends, "Monthly History Trend", "Last 6 calendar months")
            }
        }

        AnalyticsState(
            selectedDateFilter = dateFilter,
            selectedCategory = catFilter,
            searchQuery = query,
            selectedYear = year,
            selectedMonth = month,
            customStartMillis = startTime,
            customEndMillis = endTime,
            periodTitle = periodTitle,
            periodSubtitle = periodSubtitle,
            totalFilteredSpend = totalSpend,
            totalExpenseCount = filtered.size,
            dailyAverageInPeriod = dailyAverage,
            daysInPeriod = daysInPeriod,
            categoryBreakdown = breakdown,
            spendingTrends = trendData,
            trendChartTitle = chartTitle,
            trendChartSubtitle = chartSubtitle,
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
        if (filter == DateRangeFilter.THIS_MONTH) {
            val cal = Calendar.getInstance()
            selectedYear.value = cal.get(Calendar.YEAR)
            selectedMonth.value = cal.get(Calendar.MONTH)
        } else if (filter == DateRangeFilter.LAST_MONTH) {
            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
            selectedYear.value = cal.get(Calendar.YEAR)
            selectedMonth.value = cal.get(Calendar.MONTH)
        }
    }

    fun setSpecificMonth(year: Int, month: Int) {
        selectedYear.value = year
        selectedMonth.value = month
        selectedDateFilter.value = DateRangeFilter.SPECIFIC_MONTH
    }

    fun selectPreviousMonth() {
        var m = selectedMonth.value - 1
        var y = selectedYear.value
        if (m < 0) {
            m = 11
            y -= 1
        }
        selectedYear.value = y
        selectedMonth.value = m
        selectedDateFilter.value = DateRangeFilter.SPECIFIC_MONTH
    }

    fun selectNextMonth() {
        var m = selectedMonth.value + 1
        var y = selectedYear.value
        if (m > 11) {
            m = 0
            y += 1
        }
        selectedYear.value = y
        selectedMonth.value = m
        selectedDateFilter.value = DateRangeFilter.SPECIFIC_MONTH
    }

    fun setCustomRange(startMillis: Long, endMillis: Long) {
        customRangeStart.value = startMillis
        customRangeEnd.value = endMillis
        selectedDateFilter.value = DateRangeFilter.CUSTOM
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
            repository.updateUserSettings(current.copy(monthlyBudget = newBudget.coerceAtLeast(0.0)))
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
            val bill = RecurringBillEntity(
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

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
        }
    }

    fun markBillAsPaid(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.logBillAsPaid(bill)
        }
    }

    fun restoreBackupData(
        expenses: List<ExpenseEntity>,
        bills: List<RecurringBillEntity>,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            repository.restoreBackupData(expenses, bills)
            onComplete(expenses.size)
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
