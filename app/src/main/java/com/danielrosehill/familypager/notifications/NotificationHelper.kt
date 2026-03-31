package com.danielrosehill.familypager.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.danielrosehill.familypager.MainActivity

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val emergencyChannel = NotificationChannel(
            CHANNEL_EMERGENCY,
            "Emergency Pages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority emergency pages"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val normalChannel = NotificationChannel(
            CHANNEL_PAGES,
            "Pages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Call me ASAP pages"
            enableVibration(true)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Listener Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Family Pager background listener"
        }

        notificationManager.createNotificationChannel(emergencyChannel)
        notificationManager.createNotificationChannel(normalChannel)
        notificationManager.createNotificationChannel(serviceChannel)
    }

    fun showPageNotification(title: String, message: String, priority: Int) {
        val channel = if (priority >= 2) CHANNEL_EMERGENCY else CHANNEL_PAGES

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title.ifBlank { "Family Pager" })
            .setContentText(message)
            .setPriority(
                if (priority >= 2) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(
                if (priority >= 2) longArrayOf(0, 500, 200, 500, 200, 500)
                else longArrayOf(0, 300, 100, 300)
            )
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun buildServiceNotification(): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Family Pager")
            .setContentText("Listening for pages…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_EMERGENCY = "family_pager_emergency"
        const val CHANNEL_PAGES = "family_pager_pages"
        const val CHANNEL_SERVICE = "family_pager_service"
        const val SERVICE_NOTIFICATION_ID = 1001
    }
}
