package com.naigebao.network.sync

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.model.message.SyncEvent
import com.naigebao.model.session.Session
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

sealed class SyncAction {
    data class SessionAdded(val session: Session) : SyncAction()
    data class SessionUpdated(val session: Session) : SyncAction()
    data class SessionRemoved(val sessionId: String) : SyncAction()
    data class MessageReceived(val sessionId: String, val message: Message) : SyncAction()
    data class ConnectionChanged(val status: String?) : SyncAction()
    data class Unknown(val type: String, val payload: JsonObject) : SyncAction()
}

class SyncProtocol {
    fun parse(event: SyncEvent): SyncAction {
        return when (event.type) {
            "session-added" -> parseSession(event.payload)?.let { SyncAction.SessionAdded(it) }
                ?: SyncAction.Unknown(event.type, event.payload)
            "session-updated" -> parseSession(event.payload)?.let { SyncAction.SessionUpdated(it) }
                ?: SyncAction.Unknown(event.type, event.payload)
            "session-removed" -> {
                val sessionId = event.payload.string("sessionId") ?: event.payload.string("id")
                if (sessionId != null) SyncAction.SessionRemoved(sessionId)
                else SyncAction.Unknown(event.type, event.payload)
            }
            "message-received" -> parseMessage(event.payload)?.let { SyncAction.MessageReceived(it.first, it.second) }
                ?: SyncAction.Unknown(event.type, event.payload)
            "connection-changed" -> SyncAction.ConnectionChanged(
                event.payload.string("status")
            )
            else -> SyncAction.Unknown(event.type, event.payload)
        }
    }

    fun buildSyncRequest(lastSyncAt: Long?): SyncEvent {
        val payload = buildJsonObject(
            "lastSyncAt" to lastSyncAt?.let { JsonPrimitive(it) },
            "requestedAt" to JsonPrimitive(System.currentTimeMillis())
        )
        return SyncEvent(type = "sync-request", payload = payload)
    }

    private fun parseSession(payload: JsonObject): Session? {
        val sessionId = payload.string("sessionId") ?: payload.string("id") ?: return null
        val title = payload.string("title") ?: payload.string("name") ?: "Session $sessionId"
        val type = payload.string("type") ?: "default"
        val updatedAt = payload.long("updatedAt") ?: System.currentTimeMillis()
        val unreadCount = payload.int("unreadCount") ?: 0
        val lastMessageId = payload.string("lastMessageId")
        val lastSyncedAt = payload.long("lastSyncedAt")
        return Session(
            id = sessionId,
            type = type,
            title = title,
            lastMessageId = lastMessageId,
            unreadCount = unreadCount,
            updatedAt = updatedAt,
            lastSyncedAt = lastSyncedAt
        )
    }

    private fun parseMessage(payload: JsonObject): Pair<String, Message>? {
        val messageNode = payload["message"]?.jsonObject ?: payload
        val sessionId = payload.string("sessionId")
            ?: messageNode.string("sessionId")
            ?: payload.string("sid")
            ?: return null
        val messageId = messageNode.string("id")
            ?: messageNode.string("messageId")
            ?: "${sessionId}-${System.currentTimeMillis()}"
        val content = messageNode.string("content")
            ?: messageNode["content"]?.toString()
            ?: messageNode["data"]?.toString()
            ?: ""
        val senderId = messageNode.string("senderId") ?: messageNode.string("from") ?: "server"
        val timestamp = messageNode.long("createdAt")
            ?: messageNode.long("timestamp")
            ?: System.currentTimeMillis()
        val messageType = messageNode.string("type").toMessageType()
        val status = messageNode.string("status").toMessageStatus()
        val flakeId = messageNode.string("flakeId")
        return sessionId to Message(
            id = messageId,
            sessionId = sessionId,
            content = content,
            senderId = senderId,
            timestamp = timestamp,
            type = messageType,
            status = status,
            flakeId = flakeId
        )
    }

    private fun buildJsonObject(vararg entries: Pair<String, JsonPrimitive?>): JsonObject {
        val map = entries
            .filter { it.second != null }
            .associate { it.first to it.second!! }
        return JsonObject(map)
    }

    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(key: String): Long? {
        val primitive = get(key)?.jsonPrimitive ?: return null
        return primitive.longOrNull ?: primitive.doubleOrNull?.toLong()
    }

    private fun JsonObject.int(key: String): Int? {
        val primitive = get(key)?.jsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.doubleOrNull?.toInt()
    }

    private fun String?.toMessageType(): MessageType {
        return when (this?.lowercase()) {
            "image" -> MessageType.IMAGE
            "file" -> MessageType.FILE
            else -> MessageType.TEXT
        }
    }

    private fun String?.toMessageStatus(): MessageStatus {
        return when (this?.lowercase()) {
            "sent" -> MessageStatus.SENT
            "delivered" -> MessageStatus.DELIVERED
            "read" -> MessageStatus.READ
            "failed" -> MessageStatus.FAILED
            else -> MessageStatus.PENDING
        }
    }
}
