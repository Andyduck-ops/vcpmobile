package com.naigebao.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token sync can be wired to backend when available.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NaigebaoNotificationManager.ensureChannels(this)
        PushNotificationHandler(this).handle(message)
    }
}
