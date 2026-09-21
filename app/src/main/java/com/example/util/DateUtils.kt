package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun getStartOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfMonth(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfMonth(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfWeek(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getDaysElapsedInMonth(timeMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        return cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    }

    fun formatDate(timeMillis: Long): String {
        val todayStart = getStartOfDay()
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

        return when {
            timeMillis >= todayStart -> "Today, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeMillis))
            timeMillis >= yesterdayStart -> "Yesterday, " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeMillis))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timeMillis))
        }
    }

    fun formatShortDate(timeMillis: Long): String {
        return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMillis))
    }

    fun formatMonthYear(timeMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timeMillis))
    }

    fun getDayLabel(timeMillis: Long): String {
        return SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timeMillis))
    }
}
