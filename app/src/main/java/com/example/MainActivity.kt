package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.ExpenseDatabase
import com.example.data.repository.ExpenseRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory
import com.example.util.DailyReminderManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule smart evening reminder if user has enabled it
        if (DailyReminderManager.isReminderEnabled(applicationContext)) {
            val (hour, minute) = DailyReminderManager.getReminderTime(applicationContext)
            DailyReminderManager.scheduleDailyReminder(applicationContext, hour, minute)
        }

        val database = ExpenseDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = ExpenseRepository(database.expenseDao())
        val factory = ExpenseViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: ExpenseViewModel = viewModel(factory = factory)
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
