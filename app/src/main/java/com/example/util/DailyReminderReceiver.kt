package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule after reboot if enabled
            if (DailyReminderManager.isReminderEnabled(context)) {
                val (hour, minute) = DailyReminderManager.getReminderTime(context)
                DailyReminderManager.scheduleDailyReminder(context, hour, minute)
            }
            return
        }

        // Trigger Daily Reminder Notification
        showReminderNotification(context)

        // Reschedule for next day
        if (DailyReminderManager.isReminderEnabled(context)) {
            val (hour, minute) = DailyReminderManager.getReminderTime(context)
            DailyReminderManager.scheduleDailyReminder(context, hour, minute)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_expense_reminder_channel"
        const val NOTIFICATION_ID = 1001

        fun showReminderNotification(context: Context, isTest: Boolean = false) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Expense Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily evening reminder to record your personal expenses"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Click Intent to open MainActivity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val title = if (isTest) "🔔 Test: Daily Expense Check-in" else "🌙 Daily Expense Check-in"
            val text = if (isTest) {
                "Smart Daily Reminders are active! You'll receive this check-in every evening to log your expenses."
            } else {
                "Did you make any purchases today? Take 5 seconds to log your expenses and keep your budget in check!"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
