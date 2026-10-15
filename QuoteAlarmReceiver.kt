package com.example.nafahat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class QuoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("nafahat", Context.MODE_PRIVATE)
        val slot = intent.getIntExtra("slot", 0).coerceIn(0, 2)
        val keys = listOf("morning", "noon", "evening")
        if (!prefs.getBoolean("enabled", true)) return

        val quote = QuoteLibrary.quotes[
            (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + slot) % QuoteLibrary.quotes.size
        ]

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nafahat_daily"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "نفحات يومية", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("نفحات العلماء • ${quote.scholar}")
            .setContentText(quote.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${quote.text}\n\nالمصدر: ${quote.source}"))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(800 + slot, notification)

        val defaults = listOf(8 to 0, 13 to 0, 20 to 0)
        val parts = prefs.getString(keys[slot], "%02d:%02d".format(defaults[slot].first, defaults[slot].second))!!
            .split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: defaults[slot].first
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: defaults[slot].second
        NotificationScheduler.schedule(context, hour, minute, slot)
    }
}
