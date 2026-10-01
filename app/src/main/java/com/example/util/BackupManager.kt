package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ExpenseEntity
import com.example.data.model.RecurringBillEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    fun generateAndShareBackup(
        context: Context,
        expenses: List<ExpenseEntity>,
        recurringBills: List<RecurringBillEntity>,
        currencySymbol: String,
        monthlyBudget: Double
    ): File? {
        return try {
            val rootObj = JSONObject()
            rootObj.put("app", "ExpanseTracker")
            rootObj.put("version", 2)
            rootObj.put("exportedAtMillis", System.currentTimeMillis())
            rootObj.put("currencySymbol", currencySymbol)
            rootObj.put("monthlyBudget", monthlyBudget)

            // Expenses Array
            val expensesArray = JSONArray()
            for (expense in expenses) {
                val item = JSONObject()
                item.put("amount", expense.amount)
                item.put("category", expense.category)
                item.put("paymentMethod", expense.paymentMethod)
                item.put("dateMillis", expense.dateMillis)
                item.put("notes", expense.notes)
                expensesArray.put(item)
            }
            rootObj.put("expenses", expensesArray)

            // Recurring Bills Array
            val billsArray = JSONArray()
            for (bill in recurringBills) {
                val item = JSONObject()
                item.put("title", bill.title)
                item.put("amount", bill.amount)
                item.put("category", bill.category)
                item.put("paymentMethod", bill.paymentMethod)
                item.put("billingFrequency", bill.billingFrequency)
                item.put("dueDayOfMonth", bill.dueDayOfMonth)
                item.put("notes", bill.notes)
                billsArray.put(item)
            }
            rootObj.put("recurringBills", billsArray)

            // Write to file
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(reportsDir, "ExpanseTracker_Backup_$timestamp.json")

            FileOutputStream(file).use { out ->
                out.write(rootObj.toString(2).toByteArray(Charsets.UTF_8))
            }

            // Share via Intent
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expanse Tracker Data Backup ($timestamp)")
                putExtra(Intent.EXTRA_TEXT, "Expanse Tracker backup file with ${expenses.size} expenses.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Save or Share Backup File")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseBackupJson(jsonString: String): Pair<List<ExpenseEntity>, List<RecurringBillEntity>>? {
        return try {
            val root = JSONObject(jsonString)
            val expensesList = mutableListOf<ExpenseEntity>()
            val billsList = mutableListOf<RecurringBillEntity>()

            if (root.has("expenses")) {
                val expArray = root.getJSONArray("expenses")
                for (i in 0 until expArray.length()) {
                    val obj = expArray.getJSONObject(i)
                    expensesList.add(
                        ExpenseEntity(
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "Miscellaneous"),
                            paymentMethod = obj.optString("paymentMethod", "Cash"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            if (root.has("recurringBills")) {
                val billArray = root.getJSONArray("recurringBills")
                for (i in 0 until billArray.length()) {
                    val obj = billArray.getJSONObject(i)
                    billsList.add(
                        RecurringBillEntity(
                            title = obj.optString("title", "Subscription"),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "Bills"),
                            paymentMethod = obj.optString("paymentMethod", "UPI"),
                            billingFrequency = obj.optString("billingFrequency", "Monthly"),
                            dueDayOfMonth = obj.optInt("dueDayOfMonth", 1),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            Pair(expensesList, billsList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
