package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExpenseCategory(
    val title: String,
    val color: Color,
    val icon: ImageVector,
    val defaultLimit: Double
) {
    FOOD("Food", Color(0xFFFF6584), Icons.Default.Restaurant, 600.0),
    TRANSPORT("Transport", Color(0xFF3B82F6), Icons.Default.DirectionsCar, 300.0),
    UTILITIES("Utilities", Color(0xFFF59E0B), Icons.Default.AccountBalance, 250.0),
    ENTERTAINMENT("Entertainment", Color(0xFF8B5CF6), Icons.Default.Movie, 200.0),
    SHOPPING("Shopping", Color(0xFFEC4899), Icons.Default.LocalMall, 400.0),
    HEALTH("Health", Color(0xFF10B981), Icons.Default.FitnessCenter, 200.0),
    BILLS("Bills", Color(0xFF0D9488), Icons.Default.ReceiptLong, 400.0),
    MISCELLANEOUS("Miscellaneous", Color(0xFF64748B), Icons.Default.MoreHoriz, 150.0);

    companion object {
        fun fromString(name: String): ExpenseCategory {
            return entries.firstOrNull { it.title.equals(name, ignoreCase = true) } ?: MISCELLANEOUS
        }

        val allCategories = entries.toList()
    }
}

enum class PaymentMethod(
    val title: String,
    val icon: ImageVector
) {
    CASH("Cash", Icons.Default.Payments),
    CREDIT_CARD("Credit Card", Icons.Default.CreditCard),
    DEBIT_CARD("Debit Card", Icons.Default.Payment),
    UPI_ONLINE("UPI/Online", Icons.Default.QrCode);

    companion object {
        fun fromString(name: String): PaymentMethod {
            return entries.firstOrNull { it.title.equals(name, ignoreCase = true) } ?: CASH
        }

        val allMethods = entries.toList()
    }
}
