package com.naigebao.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NaigebaoNotificationManager(private val context: Context) {
    fun showNotification(
        channelId: String,
        title: String,
        body: String,
        pendingIntent: android.app.PendingIntent?
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(body.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_URGENT = "urgent"
        const val CHANNEL_HIGH = "high"
        const val CHANNEL_NORMAL = "normal"
        const val CHANNEL_SILENT = "silent"

        fun ensureChannels(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(CHANNEL_URGENT, "Urgent", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_HIGH, "High Priority", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_NORMAL, "Normal", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHANNEL_SILENT, "Silent", NotificationManager.IMPORTANCE_MIN)
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }

        fun channelForPriority(priority: String?): String {
            return when (priority?.lowercase()) {
                "urgent" -> CHANNEL_URGENT
                "high" -> CHANNEL_HIGH
                "silent" -> CHANNEL_SILENT
                else -> CHANNEL_NORMAL
            }
        }
    }
}
