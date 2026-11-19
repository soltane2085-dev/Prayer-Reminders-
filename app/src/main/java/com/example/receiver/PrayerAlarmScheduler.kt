package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calculator.AsrJuristicMethod
import com.example.calculator.CalculationMethod
import com.example.calculator.CityPreset
import com.example.calculator.PrayerTimeCalculator
import java.util.Calendar

object PrayerAlarmScheduler {

    private const val PREFS_NAME = "prayer_settings"
    private const val ALARM_REQ_CODE = 9991

    fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Load details from SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cityIdx = prefs.getInt("city_preset_idx", 0) // default Mecca
        val customLocEnabled = prefs.getBoolean("custom_loc_enabled", false)

        val lat: Double
        val lon: Double
        val tz: Double
        val cityName: String

        if (customLocEnabled) {
            lat = prefs.getFloat("custom_lat", 21.3891f).toDouble()
            lon = prefs.getFloat("custom_lon", 39.8579f).toDouble()
            tz = prefs.getFloat("custom_tz", 3.0f).toDouble()
            cityName = prefs.getString("custom_city_name", "Custom Location") ?: "Custom Location"
        } else {
            val preset = CityPreset.presets.getOrElse(cityIdx) { CityPreset.presets[0] }
            lat = preset.latitude
            lon = preset.longitude
            tz = preset.timezone
            cityName = preset.cityName
        }

        val methodStr = prefs.getString("calc_method", CalculationMethod.MWL.name) ?: CalculationMethod.MWL.name
        val method = try { CalculationMethod.valueOf(methodStr) } catch (e: Exception) { CalculationMethod.MWL }

        val asrStr = prefs.getString("asr_method", AsrJuristicMethod.STANDARD.name) ?: AsrJuristicMethod.STANDARD.name
        val asrMethod = try { AsrJuristicMethod.valueOf(asrStr) } catch (e: Exception) { AsrJuristicMethod.STANDARD }

        // Alert flags for individual prayers
        val notifyFajr = prefs.getBoolean("notify_Fajr", true)
        val notifyDhuhr = prefs.getBoolean("notify_Dhuhr", true)
        val notifyAsr = prefs.getBoolean("notify_Asr", true)
        val notifyMaghrib = prefs.getBoolean("notify_Maghrib", true)
        val notifyIsha = prefs.getBoolean("notify_Isha", true)

        // 2. Compute prayer times for today
        val now = Calendar.getInstance()
        val calculator = PrayerTimeCalculator()
        val timesToday = calculator.calculateTimes(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH),
            lat, lon, tz, method, asrMethod
        )

        // Compute times for tomorrow
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val timesTomorrow = calculator.calculateTimes(
            tomorrow.get(Calendar.YEAR),
            tomorrow.get(Calendar.MONTH) + 1,
            tomorrow.get(Calendar.DAY_OF_MONTH),
            lat, lon, tz, method, asrMethod
        )

        // 3. Assemble target candidate calendar items
        val candidates = mutableListOf<Pair<String, Calendar>>()

        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { prayer ->
            // check alert setting
            val notifyFlag = when (prayer) {
                "Fajr" -> notifyFajr
                "Dhuhr" -> notifyDhuhr
                "Asr" -> notifyAsr
                "Maghrib" -> notifyMaghrib
                "Isha" -> notifyIsha
                else -> false
            }

            if (notifyFlag) {
                // Today's prayer calendar
                timesToday[prayer]?.let { timeStr ->
                    val cal = getCalendarForTimeStr(now, timeStr)
                    if (cal.after(now)) {
                        candidates.add(Pair(prayer, cal))
                    }
                }

                // Tomorrow's prayer calendar
                timesTomorrow[prayer]?.let { timeStr ->
                    val cal = getCalendarForTimeStr(tomorrow, timeStr)
                    candidates.add(Pair(prayer, cal))
                }
            }
        }

        // 4. Find the closest future candidate
        candidates.sortBy { it.second.timeInMillis }
        val nextChoice = candidates.firstOrNull()

        if (nextChoice != null) {
            val prayerName = nextChoice.first
            val prayerCal = nextChoice.second
            val formattedTime = String.format("%02d:%02d", prayerCal.get(Calendar.HOUR_OF_DAY), prayerCal.get(Calendar.MINUTE))

            // Build Alarm Intent
            val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
                putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, prayerName)
                putExtra(PrayerReminderReceiver.EXTRA_PRAYER_TIME, formattedTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel any prior scheduled alarm just in case
            alarmManager.cancel(pendingIntent)

            // Set the alarm with try-catch to avoid Exact Alarm Permission or other scheduling errors
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayerCal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayerCal.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d("PrayerScheduler", "Successfully scheduled alarm for $prayerName in: ${prayerCal.time}")
            } catch (e: Throwable) {
                Log.e("PrayerScheduler", "Error scheduling exact or preferred alarm, falling back to setAndAllowWhileIdle", e)
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        prayerCal.timeInMillis,
                        pendingIntent
                    )
                } catch (ex: Throwable) {
                    Log.e("PrayerScheduler", "Fatal fallback alarm failure", ex)
                }
            }
        } else {
            Log.d("PrayerScheduler", "No future prayers are notification-enabled.")
        }
    }

    private fun getCalendarForTimeStr(baseCal: Calendar, timeStr: String): Calendar {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

        return Calendar.getInstance().apply {
            timeInMillis = baseCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Immediately trigger a test alarm in 5 seconds
    fun scheduleTestNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val testCal = Calendar.getInstance().apply { add(Calendar.SECOND, 5) }

        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, "Test Reminder")
            putExtra(PrayerReminderReceiver.EXTRA_PRAYER_TIME, "Just Now")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQ_CODE + 1, // unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                testCal.timeInMillis,
                pendingIntent
            )
            Log.d("PrayerScheduler", "Scheduled test alarm for 5 seconds from now")
        } catch (e: Throwable) {
            Log.e("PrayerScheduler", "Error scheduling test notification alarm", e)
        }
    }
}
