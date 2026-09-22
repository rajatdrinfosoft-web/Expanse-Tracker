package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.components.PdfExportDialog
import com.example.ui.viewmodel.ExpenseViewModel

enum class MainTab(val title: String) {
    DASHBOARD("Dashboard"),
    SUBSCRIPTIONS("Bills"),
    ANALYTICS("Analytics"),
    SETTINGS("Settings")
}

@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
        return
    }

    val dashboardMetrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val recurringBills by viewModel.recurringBills.collectAsStateWithLifecycle()
    val analyticsState by viewModel.analyticsState.collectAsStateWithLifecycle()
    val isAddSheetOpen by viewModel.isAddExpenseSheetOpen.collectAsStateWithLifecycle()
    val editingExpense by viewModel.editingExpense.collectAsStateWithLifecycle()
    val isPdfExportDialogOpen by viewModel.isPdfExportDialogOpen.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddExpense() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_expense")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp)
            ) {
                // Tab 0: Dashboard
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 0) Icons.Default.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_dashboard")
                )

                // Tab 1: Subscriptions & Recurring Bills
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 1) Icons.Default.EventRepeat else Icons.Outlined.EventRepeat,
                            contentDescription = "Bills"
                        )
                    },
                    label = {
                        Text(
                            text = "Bills",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_subscriptions")
                )

                // Tab 2: Analytics
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 2) Icons.Default.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "Analytics"
                        )
                    },
                    label = {
                        Text(
                            text = "Analytics",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_analytics")
                )

                // Tab 3: Settings
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTabIndex == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = {
                        Text(
                            text = "Settings",
                            fontWeight = if (selectedTabIndex == 3) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> DashboardScreen(
                        metrics = dashboardMetrics,
                        recentExpenses = allExpenses,
                        onAddExpenseClick = { viewModel.openAddExpense() },
                        onExpenseClick = { expense -> viewModel.openAddExpense(expense) },
                        onDeleteExpense = { expense -> viewModel.deleteExpense(expense) }
                    )
                    1 -> SubscriptionsScreen(
                        viewModel = viewModel,
                        recurringBills = recurringBills,
                        currencySymbol = dashboardMetrics.currencySymbol
                    )
                    2 -> AnalyticsScreen(
                        state = analyticsState,
                        currencySymbol = dashboardMetrics.currencySymbol,
                        onDateFilterSelected = { viewModel.setDateFilter(it) },
                        onCategoryFilterSelected = { viewModel.setCategoryFilter(it) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onExpenseClick = { expense -> viewModel.openAddExpense(expense) },
                        onDeleteExpense = { expense -> viewModel.deleteExpense(expense) },
                        onExportPdfClick = { viewModel.openPdfExportDialog() }
                    )
                    3 -> SettingsScreen(
                        monthlyBudget = dashboardMetrics.monthlyBudget,
                        currencySymbol = dashboardMetrics.currencySymbol,
                        categoryBudgets = categoryBudgets,
                        allExpenses = allExpenses,
                        onUpdateMonthlyBudget = { viewModel.updateMonthlyBudget(it) },
                        onUpdateCurrencySymbol = { viewModel.updateCurrencySymbol(it) },
                        onUpdateCategoryBudget = { cat, limit -> viewModel.updateCategoryBudget(cat, limit) },
                        onClearAllExpenses = { viewModel.clearAllExpenses() },
                        onExportPdfClick = { viewModel.openPdfExportDialog() }
                    )
                }
            }
        }

        // Add / Edit Expense Bottom Sheet Modal
        AddExpenseBottomSheet(
            isOpen = isAddSheetOpen,
            editingExpense = editingExpense,
            currencySymbol = dashboardMetrics.currencySymbol,
            onDismiss = { viewModel.closeAddExpense() },
            onSave = { amount, category, paymentMethod, dateMillis, notes ->
                viewModel.saveExpense(amount, category, paymentMethod, dateMillis, notes)
            }
        )

        // Professional PDF Export Dialog with Date Range Selection
        PdfExportDialog(
            isOpen = isPdfExportDialogOpen,
            onDismiss = { viewModel.closePdfExportDialog() },
            allExpenses = allExpenses,
            currencySymbol = dashboardMetrics.currencySymbol
        )
    }
}
