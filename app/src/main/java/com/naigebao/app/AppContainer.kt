package com.naigebao.app

import android.content.Context
import com.naigebao.app.sync.AppSyncEventHandler
import com.naigebao.auth.device.DeviceFingerprintCollector
import com.naigebao.auth.qr.QrCodeManager
import com.naigebao.auth.repository.AuthRepository
import com.naigebao.auth.token.EncryptedTokenStore
import com.naigebao.auth.token.TokenManager
import com.naigebao.network.chat.ChatService
import com.naigebao.network.chat.ChatServiceImpl
import com.naigebao.network.sync.SyncManager
import com.naigebao.network.websocket.WebSocketManager
import com.naigebao.storage.database.NaigebaoDatabase
import com.naigebao.storage.cleanup.DataCleanupManager
import com.naigebao.storage.deduplicate.MessageDeduplicator
import com.naigebao.storage.offline.MessageResendManager
import com.naigebao.storage.offline.OfflineMessageQueue
import com.naigebao.storage.repository.MessageRepository
import com.naigebao.storage.repository.RoomMessageRepository
import com.naigebao.storage.repository.RoomSessionRepository
import com.naigebao.storage.repository.SessionRepository
import com.naigebao.storage.sync.DataStoreSyncStateStore
import okhttp3.OkHttpClient
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import com.naigebao.model.message.SyncEvent

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val okHttpClient = OkHttpClient.Builder().build()

    val webSocketManager = WebSocketManager(okHttpClient)
    val database = NaigebaoDatabase.create(applicationContext)
    val messageRepository: MessageRepository = RoomMessageRepository(database.messageDao())
    val sessionRepository: SessionRepository = RoomSessionRepository(database.sessionDao())
    val syncStateStore = DataStoreSyncStateStore(applicationContext)
    private val messageDeduplicator = MessageDeduplicator()
    private val syncHandler = AppSyncEventHandler(sessionRepository, messageRepository)

    val syncManager = SyncManager(
        webSocketManager = webSocketManager,
        syncStateStore = syncStateStore,
        dedupe = messageDeduplicator::shouldProcess,
        handler = syncHandler
    )

    // ChatService for message operations
    val chatService: ChatService = ChatServiceImpl(
        messageRepository = messageRepository,
        sessionRepository = sessionRepository,
        webSocketManager = webSocketManager
    )

    val tokenManager = TokenManager(EncryptedTokenStore(applicationContext))
    val qrCodeManager = QrCodeManager()
    val authRepository = AuthRepository(tokenManager, qrCodeManager)

    val deviceFingerprint = DeviceFingerprintCollector.collect(applicationContext)

    val offlineMessageQueue = OfflineMessageQueue(messageRepository)
    val messageResendManager = MessageResendManager(
        messageRepository = messageRepository,
        sender = { message ->
            val payload = buildJsonObject {
                put("sessionId", JsonPrimitive(message.sessionId))
                put("messageId", JsonPrimitive(message.id))
                put("content", JsonPrimitive(message.content))
                put("timestamp", JsonPrimitive(message.timestamp))
            }
            webSocketManager.send(SyncEvent(type = "message-send", payload = payload, flakeId = message.flakeId))
        }
    )

    val cleanupManager = DataCleanupManager(database.sessionDao(), database.messageDao())
}