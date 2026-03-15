package com.naigebao.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.RemoteMessage

class PushNotificationHandler(
    private val context: Context,
    private val notificationManager: NaigebaoNotificationManager = NaigebaoNotificationManager(context)
) {
    fun handle(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Naigebao"
        val body = message.notification?.body ?: data["body"] ?: "New message"
        val priority = data["priority"]
        val sessionId = data["sessionId"]

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (sessionId != null) {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        } ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationManager.showNotification(
            channelId = NaigebaoNotificationManager.channelForPriority(priority),
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )
    }

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
    }
}
