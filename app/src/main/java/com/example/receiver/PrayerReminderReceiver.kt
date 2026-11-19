package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.calculator.AsrJuristicMethod
import com.example.calculator.CalculationMethod
import com.example.calculator.PrayerTimeCalculator
import java.util.Calendar

class PrayerReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_STOP_ADHAN) {
            Log.d("PrayerReceiver", "Received stop adhan intent")
            AdhanAudioPlayer.stop()
            val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: ""
            if (prayerName.isNotEmpty()) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(prayerName.hashCode())
            }
            return
        }

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val nextTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        Log.d("PrayerReceiver", "Alarm received for: $prayerName, time: $nextTime")

        // 1. Create notification channel
        createNotificationChannel(context)

        // Play the sound based on user preference
        val prefs = context.getSharedPreferences("prayer_settings", Context.MODE_PRIVATE)
        val soundType = prefs.getString("alert_sound_type", "ADHAN_SHORT") ?: "ADHAN_SHORT"
        AdhanAudioPlayer.play(context, soundType)

        // 2. Build content intent to launch MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Show notification
        try {
            val stopIntent = Intent(context, PrayerReminderReceiver::class.java).apply {
                setAction(ACTION_STOP_ADHAN)
                putExtra(EXTRA_PRAYER_NAME, prayerName)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                prayerName.hashCode() + 101,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // simple system icon, visible on status bar
                .setContentTitle("Sajda: Time for $prayerName")
                .setContentText(if (nextTime.isNotEmpty()) "It is now time for $prayerName ($nextTime). Tap to open reminders." else "It is now time for $prayerName.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (soundType != "SILENT") {
                notificationBuilder.addAction(
                    android.R.drawable.ic_media_pause,
                    "إيقاف الصوت",
                    stopPendingIntent
                )
            }

            notificationManager.notify(prayerName.hashCode(), notificationBuilder.build())
        } catch (e: Throwable) {
            Log.e("PrayerReceiver", "Error while showing notification for $prayerName", e)
        }

        // 4. Continuously chain: Schedule the next upcoming prayer alarm!
        try {
            PrayerAlarmScheduler.scheduleNextAlarm(context)
        } catch (e: Throwable) {
            Log.e("PrayerReceiver", "Error chaining next alarm scheduling", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val name = "Prayer Reminders"
                val descriptionText = "Get notifications for prayer times"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            } catch (e: Throwable) {
                Log.e("PrayerReceiver", "Error creating notification channel", e)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "prayer_reminders_channel"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val ACTION_STOP_ADHAN = "com.example.ACTION_STOP_ADHAN"
    }
}
