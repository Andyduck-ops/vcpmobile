package com.naigebao.app.sync

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.model.message.SyncEvent
import com.naigebao.model.session.Session
import com.naigebao.network.sync.SyncEventHandler
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.repository.MessageRepository
import com.naigebao.storage.repository.SessionRepository

class AppSyncEventHandler(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository
) : SyncEventHandler {
    override suspend fun onSessionAdded(session: Session) {
        sessionRepository.upsert(session.toEntity())
    }

    override suspend fun onSessionUpdated(session: Session) {
        sessionRepository.upsert(session.toEntity())
    }

    override suspend fun onSessionRemoved(sessionId: String) {
        sessionRepository.deleteById(sessionId)
    }

    override suspend fun onMessageReceived(sessionId: String, message: Message) {
        val saved = message.copy(status = MessageStatus.SENT)
        messageRepository.upsert(saved)
        updateSessionPreview(sessionId, saved.content, saved.timestamp)
    }

    override suspend fun onConnectionChanged(status: String?) {
        // No-op for now; state is persisted by SyncManager.
    }

    override suspend fun onUnknown(event: SyncEvent) {
        val content = event.payload.toString()
        val fallbackSession = Session(
            id = SYSTEM_SESSION_ID,
            type = "system",
            title = "VCPLog",
            updatedAt = System.currentTimeMillis()
        )
        sessionRepository.upsert(fallbackSession.toEntity())
        val message = Message(
            id = "${SYSTEM_SESSION_ID}-${event.receivedAt}",
            sessionId = SYSTEM_SESSION_ID,
            content = content,
            senderId = "server",
            timestamp = event.receivedAt,
            type = MessageType.TEXT,
            status = MessageStatus.SENT,
            flakeId = event.flakeId
        )
        messageRepository.upsert(message)
        updateSessionPreview(SYSTEM_SESSION_ID, content, event.receivedAt)
    }

    private suspend fun updateSessionPreview(sessionId: String, preview: String, timestamp: Long) {
        val existing = sessionRepository.getSessionById(sessionId)
        val updated = SessionEntity(
            id = sessionId,
            type = existing?.type ?: "default",
            title = existing?.title ?: "Chat $sessionId",
            lastMessageId = existing?.lastMessageId,
            lastMessagePreview = preview,
            unreadCount = (existing?.unreadCount ?: 0) + 1,
            updatedAt = timestamp,
            lastSyncedAt = existing?.lastSyncedAt
        )
        sessionRepository.upsert(updated)
    }

    private fun Session.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            type = type,
            title = title,
            lastMessageId = lastMessageId,
            lastMessagePreview = lastMessagePreview,
            unreadCount = unreadCount,
            updatedAt = updatedAt,
            lastSyncedAt = lastSyncedAt,
            forkSourceSessionId = forkSourceSessionId,
            forkSourceMessageId = forkSourceMessageId,
            isPinned = isPinned
        )
    }

    companion object {
        private const val SYSTEM_SESSION_ID = "vcplog"
    }
}
