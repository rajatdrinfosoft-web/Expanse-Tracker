package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryBudgetEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
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
                    monthlyBudget = 2500.0,
                    currencySymbol = "$"
                )
            )

            // Default category budgets
            val defaultBudgets = ExpenseCategory.allCategories.map { cat ->
                CategoryBudgetEntity(
                    categoryName = cat.title,
                    monthlyLimit = cat.defaultLimit
                )
            }
            dao.insertCategoryBudgets(defaultBudgets)

            // Seed realistic sample expenses for the current month
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            fun daysAgo(days: Int, hour: Int = 12): Long {
                val c = Calendar.getInstance()
                c.add(Calendar.DAY_OF_YEAR, -days)
                c.set(Calendar.HOUR_OF_DAY, hour)
                c.set(Calendar.MINUTE, 30)
                return c.timeInMillis
            }

            val sampleExpenses = listOf(
                ExpenseEntity(
                    amount = 45.50,
                    category = ExpenseCategory.FOOD.title,
                    paymentMethod = PaymentMethod.CREDIT_CARD.title,
                    dateMillis = now - 1000 * 60 * 90, // 1.5 hours ago
                    notes = "Organic Grocery Market"
                ),
                ExpenseEntity(
                    amount = 18.25,
                    category = ExpenseCategory.TRANSPORT.title,
                    paymentMethod = PaymentMethod.UPI_ONLINE.title,
                    dateMillis = now - 1000 * 60 * 360, // 6 hours ago
                    notes = "Metro Rail Pass"
                ),
                ExpenseEntity(
                    amount = 12.00,
                    category = ExpenseCategory.FOOD.title,
                    paymentMethod = PaymentMethod.CASH.title,
                    dateMillis = daysAgo(1, 9),
                    notes = "Morning Artisan Coffee"
                ),
                ExpenseEntity(
                    amount = 79.99,
                    category = ExpenseCategory.SHOPPING.title,
                    paymentMethod = PaymentMethod.CREDIT_CARD.title,
                    dateMillis = daysAgo(1, 16),
                    notes = "Running shoes & socks"
                ),
                ExpenseEntity(
                    amount = 65.00,
                    category = ExpenseCategory.UTILITIES.title,
                    paymentMethod = PaymentMethod.DEBIT_CARD.title,
                    dateMillis = daysAgo(2, 11),
                    notes = "High-speed Internet Bill"
                ),
                ExpenseEntity(
                    amount = 28.50,
                    category = ExpenseCategory.FOOD.title,
                    paymentMethod = PaymentMethod.UPI_ONLINE.title,
                    dateMillis = daysAgo(3, 13),
                    notes = "Lunch with colleagues"
                ),
                ExpenseEntity(
                    amount = 24.00,
                    category = ExpenseCategory.ENTERTAINMENT.title,
                    paymentMethod = PaymentMethod.CREDIT_CARD.title,
                    dateMillis = daysAgo(4, 20),
                    notes = "Cinema tickets"
                ),
                ExpenseEntity(
                    amount = 35.00,
                    category = ExpenseCategory.HEALTH.title,
                    paymentMethod = PaymentMethod.DEBIT_CARD.title,
                    dateMillis = daysAgo(5, 15),
                    notes = "Vitamin supplements"
                ),
                ExpenseEntity(
                    amount = 120.00,
                    category = ExpenseCategory.BILLS.title,
                    paymentMethod = PaymentMethod.UPI_ONLINE.title,
                    dateMillis = daysAgo(7, 10),
                    notes = "Electricity & Power bill"
                ),
                ExpenseEntity(
                    amount = 22.00,
                    category = ExpenseCategory.TRANSPORT.title,
                    paymentMethod = PaymentMethod.CASH.title,
                    dateMillis = daysAgo(8, 18),
                    notes = "Cab ride to airport"
                ),
                ExpenseEntity(
                    amount = 115.00,
                    category = ExpenseCategory.FOOD.title,
                    paymentMethod = PaymentMethod.CREDIT_CARD.title,
                    dateMillis = daysAgo(10, 19),
                    notes = "Weekly family supermarket"
                ),
                ExpenseEntity(
                    amount = 15.00,
                    category = ExpenseCategory.MISCELLANEOUS.title,
                    paymentMethod = PaymentMethod.CASH.title,
                    dateMillis = daysAgo(12, 14),
                    notes = "Notebook and desk accessories"
                )
            )

            dao.insertExpenses(sampleExpenses)
        }
    }
}
