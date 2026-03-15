package com.naigebao.network.chat

import com.naigebao.common.FlakeIdGenerator
import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.model.message.SyncEvent
import com.naigebao.model.session.Session
import com.naigebao.network.websocket.WebSocketManager
import com.naigebao.storage.database.dao.MessageDao
import com.naigebao.storage.database.dao.SessionDao
import com.naigebao.storage.database.entity.MessageEntity
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.repository.MessageRepository
import com.naigebao.storage.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

/**
 * Implementation of ChatService
 * Provides message operations with WebSocket communication
 */
class ChatServiceImpl(
    private val messageRepository: MessageRepository,
    private val sessionRepository: SessionRepository,
    private val webSocketManager: WebSocketManager,
    private val translationApi: TranslationApi? = null
) : ChatService {

    override suspend fun sendMessage(
        sessionId: String,
        content: String,
        modelId: String?,
        attachments: List<OutgoingAttachment>
    ): Message {
        val createdMessages = mutableListOf<Message>()
        val trimmedContent = content.trim()

        if (trimmedContent.isNotEmpty()) {
            createdMessages += sendSingleMessage(
                sessionId = sessionId,
                content = trimmedContent,
                type = MessageType.TEXT,
                modelId = modelId
            )
        }

        attachments.forEach { attachment ->
            val messageType = if (attachment.mimeType.startsWith("image/")) {
                MessageType.IMAGE
            } else {
                MessageType.FILE
            }
            val messageContent = if (messageType == MessageType.IMAGE) {
                attachment.uri
            } else {
                attachment.name
            }
            createdMessages += sendSingleMessage(
                sessionId = sessionId,
                content = messageContent,
                type = messageType,
                modelId = modelId,
                attachment = attachment
            )
        }

        require(createdMessages.isNotEmpty()) { "sendMessage requires text or attachments" }

        val preview = when {
            trimmedContent.isNotEmpty() -> trimmedContent
            attachments.size == 1 -> attachments.first().name
            else -> "${attachments.size} attachments"
        }
        updateSessionPreview(
            sessionId = sessionId,
            preview = preview,
            timestamp = createdMessages.maxOf { it.timestamp }
        )

        return createdMessages.firstOrNull { it.type == MessageType.TEXT } ?: createdMessages.last()
    }

    private suspend fun sendSingleMessage(
        sessionId: String,
        content: String,
        type: MessageType,
        modelId: String?,
        attachment: OutgoingAttachment? = null
    ): Message {
        val now = System.currentTimeMillis()
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            content = content,
            senderId = "me",
            timestamp = now,
            type = type,
            status = MessageStatus.PENDING,
            flakeId = FlakeIdGenerator.nextId()
        )

        messageRepository.upsert(message)

        val payload = buildJsonObject {
            put("sessionId", JsonPrimitive(sessionId))
            put("messageId", JsonPrimitive(message.id))
            put("content", JsonPrimitive(content))
            put("type", JsonPrimitive(type.name.lowercase()))
            put("timestamp", JsonPrimitive(now))
            put("flakeId", JsonPrimitive(message.flakeId ?: ""))
            modelId?.let { put("modelId", JsonPrimitive(it)) }
            attachment?.let {
                put("attachmentName", JsonPrimitive(it.name))
                put("attachmentUri", JsonPrimitive(it.uri))
                put("mimeType", JsonPrimitive(it.mimeType))
                put("size", JsonPrimitive(it.size))
            }
        }

        val event = SyncEvent(
            type = "message-send",
            payload = payload,
            flakeId = message.flakeId
        )

        val sent = webSocketManager.send(event)
        val finalStatus = if (sent) MessageStatus.SENT else MessageStatus.FAILED
        messageRepository.upsert(message.copy(status = finalStatus))

        return message.copy(status = finalStatus)
    }

    override suspend fun editMessage(messageId: String, newContent: String): Message? {
        val originalMessage = messageRepository.fetchById(messageId) ?: return null

        // Create updated message
        val updatedMessage = originalMessage.copy(
            content = newContent,
            timestamp = System.currentTimeMillis()
        )

        // Save locally
        messageRepository.upsert(updatedMessage)

        // Send edit event via WebSocket
        val payload = buildJsonObject {
            put("messageId", JsonPrimitive(messageId))
            put("content", JsonPrimitive(newContent))
            put("timestamp", JsonPrimitive(updatedMessage.timestamp))
            put("flakeId", JsonPrimitive(originalMessage.flakeId ?: ""))
        }

        val event = SyncEvent(
            type = "message-edit",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        webSocketManager.send(event)

        return updatedMessage
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        val message = messageRepository.fetchById(messageId) ?: return false

        // Send delete event via WebSocket first
        val payload = buildJsonObject {
            put("messageId", JsonPrimitive(messageId))
            put("flakeId", JsonPrimitive(message.flakeId ?: ""))
        }

        val event = SyncEvent(
            type = "message-delete",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        val sent = webSocketManager.send(event)

        if (!sent) {
            return false
        }

        // Note: Actual deletion from database should be handled by sync handler
        // For now, we mark it as deleted locally
        messageRepository.upsert(message.copy(
            content = "[deleted]",
            status = MessageStatus.FAILED // Using FAILED as a marker for deleted
        ))

        return true
    }

    override suspend fun regenerateMessage(messageId: String): Message? {
        val originalMessage = messageRepository.fetchById(messageId) ?: return null

        // Send regenerate event via WebSocket
        val payload = buildJsonObject {
            put("sessionId", JsonPrimitive(originalMessage.sessionId))
            put("originalMessageId", JsonPrimitive(messageId))
            put("flakeId", JsonPrimitive(originalMessage.flakeId ?: ""))
        }

        val event = SyncEvent(
            type = "message-regenerate",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        webSocketManager.send(event)

        // Create a new pending message for the regeneration
        val now = System.currentTimeMillis()
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            sessionId = originalMessage.sessionId,
            content = "",
            senderId = "assistant",
            timestamp = now,
            type = MessageType.TEXT,
            status = MessageStatus.PENDING,
            flakeId = FlakeIdGenerator.nextId()
        )
        messageRepository.upsert(newMessage)

        return newMessage
    }

    override suspend fun regenerateAtMessage(messageId: String): Message? {
        val originalMessage = messageRepository.fetchById(messageId) ?: return null
        val rootMessageId = originalMessage.parentMessageId ?: originalMessage.id

        messageRepository.markAsNotLatest(originalMessage.id)

        val nextVersion = messageRepository.getMaxVersion(rootMessageId) + 1

        // Send regenerate event via WebSocket with version tracking
        val payload = buildJsonObject {
            put("sessionId", JsonPrimitive(originalMessage.sessionId))
            put("originalMessageId", JsonPrimitive(messageId))
            put("parentMessageId", JsonPrimitive(rootMessageId))
            put("version", JsonPrimitive(nextVersion))
            put("flakeId", JsonPrimitive(originalMessage.flakeId ?: ""))
        }

        val event = SyncEvent(
            type = "message-regenerate",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        webSocketManager.send(event)

        // Create a new version message
        val now = System.currentTimeMillis()
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            sessionId = originalMessage.sessionId,
            content = "",
            senderId = "assistant",
            timestamp = now,
            type = MessageType.TEXT,
            status = MessageStatus.PENDING,
            flakeId = FlakeIdGenerator.nextId(),
            parentMessageId = rootMessageId,
            version = nextVersion,
            isLatestVersion = true,
            previousVersionId = originalMessage.id
        )
        messageRepository.upsert(newMessage)

        return newMessage
    }

    override suspend fun forkFromMessage(messageId: String): String? {
        // Delegate to forkConversationAtMessage with default title
        return forkConversationAtMessage(messageId, null)
    }

    override suspend fun forkConversationAtMessage(messageId: String, newSessionTitle: String?): String? {
        // Get the target message
        val targetMessage = messageRepository.fetchById(messageId) ?: return null
        val sourceSessionId = targetMessage.sessionId

        // Get all messages up to the fork point
        val messagesToFork = messageRepository.fetchMessagesUpToTimestamp(
            sourceSessionId,
            targetMessage.timestamp
        )

        // Create new session for the fork
        val newSessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val sessionTitle = newSessionTitle ?: "Forked from ${targetMessage.content.take(30)}..."
        val newSession = SessionEntity(
            id = newSessionId,
            type = "fork",
            title = sessionTitle,
            lastMessageId = null,
            lastMessagePreview = targetMessage.content.take(50),
            unreadCount = 0,
            updatedAt = now,
            lastSyncedAt = null
        )
        sessionRepository.upsert(newSession)

        // Copy messages to new session with fork source tracking
        for (msg in messagesToFork) {
            val forkedMessage = msg.copy(
                id = UUID.randomUUID().toString(),
                sessionId = newSessionId,
                status = MessageStatus.PENDING,
                // Track fork source
                forkSourceSessionId = sourceSessionId,
                forkSourceMessageId = msg.id
            )
            messageRepository.upsert(forkedMessage)
        }

        // Send fork event via WebSocket
        val payload = buildJsonObject {
            put("originalSessionId", JsonPrimitive(sourceSessionId))
            put("forkMessageId", JsonPrimitive(messageId))
            put("newSessionId", JsonPrimitive(newSessionId))
            put("forkMessageTimestamp", JsonPrimitive(targetMessage.timestamp))
        }

        val event = SyncEvent(
            type = "conversation-fork",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        webSocketManager.send(event)

        return newSessionId
    }

    override suspend fun stopGeneration(sessionId: String, requestId: String?) {
        // Cancel specific request if provided
        if (requestId != null) {
            webSocketManager.cancelRequest(requestId)
        }

        // Send stop generation event via WebSocket
        val payload = buildJsonObject {
            put("sessionId", JsonPrimitive(sessionId))
            requestId?.let { put("requestId", JsonPrimitive(it)) }
        }

        val event = SyncEvent(
            type = "generation-stop",
            payload = payload,
            flakeId = FlakeIdGenerator.nextId()
        )
        webSocketManager.send(event)

        // Update any pending messages in this session to cancelled status
        val messages = messageRepository.fetchLatest(sessionId, 100)
        messages.filter {
            it.status == MessageStatus.PENDING && it.senderId == "assistant"
        }.forEach { message ->
            messageRepository.upsert(message.copy(status = MessageStatus.CANCELLED))
        }
    }

    override suspend fun translateMessage(messageId: String, targetLanguage: String): Message? {
        return withContext(Dispatchers.IO) {
            // Get original message
            val originalMessage = messageRepository.fetchById(messageId) ?: return@withContext null

            // If already translated to this language, return cached
            if (originalMessage.translationLanguage == targetLanguage &&
                originalMessage.translatedContent != null) {
                return@withContext originalMessage
            }

            // Update status to translating
            messageRepository.upsert(originalMessage.copy(status = MessageStatus.TRANSLATING))

            try {
                // Perform translation
                val translatedContent = if (translationApi != null) {
                    translationApi.translate(originalMessage.content, targetLanguage)
                } else {
                    // Simple mock translation for demo purposes
                    // In production, replace with actual translation service
                    "[${targetLanguage.uppercase()}] ${originalMessage.content}"
                }

                // Update message with translation
                val updatedMessage = originalMessage.copy(
                    translatedContent = translatedContent,
                    translationLanguage = targetLanguage,
                    status = MessageStatus.SENT
                )
                messageRepository.upsert(updatedMessage)

                // Send translation sync event
                val payload = buildJsonObject {
                    put("messageId", JsonPrimitive(messageId))
                    put("translatedContent", JsonPrimitive(translatedContent))
                    put("language", JsonPrimitive(targetLanguage))
                    put("originalLanguage", JsonPrimitive("auto"))
                }

                val event = SyncEvent(
                    type = "message-translate",
                    payload = payload,
                    flakeId = FlakeIdGenerator.nextId()
                )
                webSocketManager.send(event)

                updatedMessage
            } catch (e: Exception) {
                // Restore original status on failure
                messageRepository.upsert(originalMessage)
                null
            }
        }
    }

    override suspend fun getMessage(messageId: String): Message? {
        return messageRepository.fetchById(messageId)
    }

    override suspend fun getMessageVersions(parentMessageId: String): List<Message> {
        val rootMessage = messageRepository.fetchById(parentMessageId) ?: return emptyList()
        val rootId = rootMessage.parentMessageId ?: rootMessage.id
        val versions = messageRepository.fetchVersions(rootId)
        return (listOfNotNull(messageRepository.fetchById(rootId)) + versions)
            .distinctBy { it.id }
            .sortedBy { it.version }
    }

    override fun observeMessages(sessionId: String): Flow<List<Message>> {
        return messageRepository.observeMessages(sessionId)
    }

    override suspend fun getMessages(sessionId: String, limit: Int): List<Message> {
        return messageRepository.fetchLatest(sessionId, limit)
    }

    override suspend fun toggleMessageFavorite(messageId: String): Message? {
        return withContext(Dispatchers.IO) {
            val message = messageRepository.fetchById(messageId) ?: return@withContext null
            val newFavoriteStatus = !message.isFavorite
            messageRepository.updateFavoriteStatus(messageId, newFavoriteStatus)
            message.copy(isFavorite = newFavoriteStatus)
        }
    }

    override suspend fun togglePinStatus(sessionId: String): SessionEntity? {
        return withContext(Dispatchers.IO) {
            val session = sessionRepository.getSessionById(sessionId) ?: return@withContext null
            val newPinnedStatus = !session.isPinned
            sessionRepository.updatePinStatus(sessionId, newPinnedStatus)
            session.copy(isPinned = newPinnedStatus)
        }
    }

    override suspend fun generateTitle(sessionId: String?): String? {
        return withContext(Dispatchers.IO) {
            val targetSessionId = sessionId ?: return@withContext null
            val session = sessionRepository.getSessionById(targetSessionId) ?: return@withContext null

            // If title already exists and not blank, return it
            if (session.title.isNotBlank()) {
                return@withContext session.title
            }

            // Try to get first user message as title
            val messages = messageRepository.fetchLatest(targetSessionId, 50)
            val firstUserMessage = messages.firstOrNull { it.senderId == "me" }

            val generatedTitle = if (firstUserMessage != null) {
                // Use first user message as title (truncated)
                firstUserMessage.content.take(50).let {
                    if (firstUserMessage.content.length > 50) "$it..." else it
                }
            } else {
                // Default title if no user messages
                "New Chat"
            }

            // Update session with generated title
            sessionRepository.upsert(session.copy(title = generatedTitle))

            generatedTitle
        }
    }

    private suspend fun updateSessionPreview(sessionId: String, preview: String, timestamp: Long) {
        val existing = sessionRepository.getSessionById(sessionId)
        sessionRepository.upsert(
            existing?.copy(
                lastMessagePreview = preview.take(100),
                updatedAt = timestamp
            ) ?: SessionEntity(
                id = sessionId,
                type = "default",
                title = "Chat $sessionId",
                lastMessageId = null,
                lastMessagePreview = preview.take(100),
                unreadCount = 0,
                updatedAt = timestamp,
                lastSyncedAt = null
            )
        )
    }
}
