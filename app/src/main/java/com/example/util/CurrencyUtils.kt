package com.example.util

import java.text.DecimalFormat
import java.util.Locale

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val compactFormat = DecimalFormat("#,##0")

    fun format(amount: Double, symbol: String = "₹"): String {
        return "$symbol${decimalFormat.format(amount)}"
    }

    fun formatCompact(amount: Double, symbol: String = "₹"): String {
        return "$symbol${compactFormat.format(amount)}"
    }
}
