package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryBudgetEntity::class,
        UserSettingsEntity::class,
        RecurringBillEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expanse_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(ExpenseDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ExpenseDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.expenseDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: ExpenseDao) {
            // Default user settings
            dao.insertOrUpdateUserSettings(
                UserSettingsEntity(
                    id = 1,
                    monthlyBudget = 0.0,
                    currencySymbol = "₹"
                )
            )

            // Default category budgets initialized to 0.0
            val defaultBudgets = ExpenseCategory.allCategories.map { cat ->
                CategoryBudgetEntity(
                    categoryName = cat.title,
                    monthlyLimit = 0.0
                )
            }
            dao.insertCategoryBudgets(defaultBudgets)

            // Sample Recurring Subscriptions & Bills
            val initialBills = listOf(
                RecurringBillEntity(
                    title = "Fiber Broadband Internet",
                    amount = 999.0,
                    category = "Bills",
                    paymentMethod = "UPI",
                    billingFrequency = "Monthly",
                    dueDayOfMonth = 5,
                    notes = "High-speed home connection"
                ),
                RecurringBillEntity(
                    title = "Netflix Premium 4K",
                    amount = 649.0,
                    category = "Entertainment",
                    paymentMethod = "Credit Card",
                    billingFrequency = "Monthly",
                    dueDayOfMonth = 15,
                    notes = "Family streaming plan"
                ),
                RecurringBillEntity(
                    title = "Gym & Fitness Membership",
                    amount = 1800.0,
                    category = "Health",
                    paymentMethod = "UPI",
                    billingFrequency = "Monthly",
                    dueDayOfMonth = 1,
                    notes = "Monthly fitness club"
                )
            )
            dao.insertRecurringBills(initialBills)
        }
    }
}
