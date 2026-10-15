package com.example.nafahat

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private val darkGreen = Color.rgb(16, 47, 43)
    private val gold = Color.rgb(201, 169, 110)
    private val cream = Color.rgb(248, 244, 232)
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("nafahat", MODE_PRIVATE)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 30, 28, 24)
            setBackgroundColor(cream)
            layoutDirection = ViewGroup.LAYOUT_DIRECTION_RTL
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        text("۞ نفحات العلماء", 30, darkGreen, true)
        text("ثلاث وقفات يومية مع أقوال العلماء", 16, Color.DKGRAY)
        text("حكمة اليوم", 21, darkGreen, true)

        val quote = QuoteLibrary.quotes[(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) % QuoteLibrary.quotes.size]
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.WHITE)
        }
        card.addView(TextView(this).apply {
            text = quote.scholar
            textSize = 20f
            setTextColor(darkGreen)
            gravity = Gravity.RIGHT
        })
        card.addView(TextView(this).apply {
            text = "«${quote.text}»"
            textSize = 19f
            setTextColor(Color.BLACK)
            setPadding(0, 18, 0, 18)
            gravity = Gravity.RIGHT
        })
        card.addView(TextView(this).apply {
            text = "المصدر: ${quote.source}\n${quote.note}"
            textSize = 13f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.RIGHT
        })
        root.addView(card, marginParams())

        text("مواعيد الإشعارات", 21, darkGreen, true)
        val times = listOf(
            Triple("الصباح", "morning", "08:00"),
            Triple("الظهيرة", "noon", "13:00"),
            Triple("المساء", "evening", "20:00")
        )
        val timeInputs = mutableListOf<Pair<String, EditText>>()
        times.forEach { (label, key, default) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val labelView = TextView(this).apply {
                text = label
                textSize = 16f
                setTextColor(darkGreen)
            }
            val input = EditText(this).apply {
                setText(prefs.getString(key, default))
                hint = "HH:mm"
                textSize = 16f
                setSingleLine(true)
                setEms(6)
            }
            row.addView(labelView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(input)
            root.addView(row, marginParams())
            timeInputs.add(key to input)
        }

        val enabled = Switch(this).apply {
            text = "تفعيل الإشعارات اليومية"
            textSize = 16f
            isChecked = prefs.getBoolean("enabled", true)
            setTextColor(darkGreen)
        }
        root.addView(enabled, marginParams())

        val save = Button(this).apply {
            text = "حفظ الإعدادات وتفعيل الجدولة"
            setTextColor(Color.WHITE)
            setBackgroundColor(darkGreen)
            setOnClickListener {
                val parsed = timeInputs.map { (key, input) ->
                    val parts = input.text.toString().trim().split(":")
                    if (parts.size != 2) null else {
                        val h = parts[0].toIntOrNull()
                        val m = parts[1].toIntOrNull()
                        if (h == null || m == null || h !in 0..23 || m !in 0..59) null else Triple(key, h, m)
                    }
                }
                if (parsed.any { it == null }) {
                    Toast.makeText(this@MainActivity, "أدخل الوقت بصيغة صحيحة مثل 08:30", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val valid = parsed.filterNotNull()
                if (valid.map { it.second * 60 + it.third }.distinct().size != 3) {
                    Toast.makeText(this@MainActivity, "اختر ثلاثة أوقات مختلفة", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                prefs.edit().putBoolean("enabled", enabled.isChecked).apply()
                valid.forEach { (key, h, m) -> prefs.edit().putString(key, "%02d:%02d".format(h, m)).apply() }
                if (enabled.isChecked) {
                    valid.forEachIndexed { slot, item -> NotificationScheduler.schedule(this@MainActivity, item.second, item.third, slot) }
                    requestNotificationPermission()
                    Toast.makeText(this@MainActivity, "تم حفظ المواعيد وجدولة الإشعارات", Toast.LENGTH_LONG).show()
                } else {
                    val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
                    (0..2).forEach { slot ->
                        val pi = android.app.PendingIntent.getBroadcast(
                            this@MainActivity, 4100 + slot,
                            android.content.Intent(this@MainActivity, QuoteAlarmReceiver::class.java),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        am.cancel(pi)
                    }
                    Toast.makeText(this@MainActivity, "تم إيقاف الإشعارات", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(save, marginParams())
        text("ملاحظة التوثيق", 18, darkGreen, true)
        text("هذه نسخة أولية. بعض العبارات أمثلة أو صياغات بالمعنى، وليست جميعها اقتباسات حرفية موثقة. راجع النصوص والمراجع قبل النشر العام.", 14, Color.DKGRAY)
        text("الإشعارات تعتمد على أذونات Android وإدارة البطارية في الجهاز.", 13, Color.DKGRAY)
        requestNotificationPermission()
        NotificationScheduler.scheduleAll(this)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) {
        root.addView(TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.RIGHT
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 12, 0, 12)
        })
    }

    private fun marginParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 8, 0, 12) }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 900)
        }
    }
}
