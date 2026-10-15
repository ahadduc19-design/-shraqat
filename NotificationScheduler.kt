package com.example.nafahat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object NotificationScheduler {
    private val defaults = listOf(
        Triple("morning", 8, 0),
        Triple("noon", 13, 0),
        Triple("evening", 20, 0)
    )

    fun scheduleAll(context: Context) {
        val prefs = context.getSharedPreferences("nafahat", Context.MODE_PRIVATE)
        defaults.forEachIndexed { slot, item ->
            val parts = prefs.getString(item.first, "%02d:%02d".format(item.second, item.third))!!
                .split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: item.second
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: item.third
            schedule(context, hour, minute, slot)
        }
    }

    fun schedule(context: Context, hour: Int, minute: Int, slot: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, QuoteAlarmReceiver::class.java).apply {
            putExtra("slot", slot)
        }
        val pending = PendingIntent.getBroadcast(
            context, 4100 + slot, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
    }
}
