package com.naigebao.storage.repository

import com.naigebao.model.message.GenerationStatus
import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.storage.database.dao.MessageDao
import com.naigebao.storage.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MessageRepository {
    fun observeMessages(sessionId: String): Flow<List<Message>>

    suspend fun fetchLatest(sessionId: String, limit: Int): List<Message>

    suspend fun fetchBefore(sessionId: String, before: Long, limit: Int): List<Message>

    suspend fun fetchByStatus(status: MessageStatus): List<Message>

    suspend fun upsert(message: Message)

    suspend fun updateLocalStatus(messageId: String, status: MessageStatus)

    suspend fun updateRetryMetadata(messageId: String, retryCount: Int, lastAttemptAt: Long?)

    suspend fun deleteMessagesBefore(before: Long)

    // Version/branch support
    suspend fun fetchVersions(parentMessageId: String): List<Message>

    suspend fun fetchById(messageId: String): Message?

    suspend fun fetchMessagesUpToTimestamp(sessionId: String, timestamp: Long): List<Message>

    suspend fun fetchByForkSource(sourceSessionId: String): List<Message>

    suspend fun getMaxVersion(parentMessageId: String): Int

    suspend fun markAsNotLatest(messageId: String)

    suspend fun updateMessageVersion(messageId: String, version: Int, previousVersionId: String)

    // Favorite support
    suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean)

    // Generation status support
    suspend fun updateGenerationStatus(
        messageId: String,
        generationStatus: GenerationStatus?,
        lastToolName: String? = null,
        lastToolInput: String? = null,
        lastToolExecutedAt: Long? = null,
        lastReasoningContent: String? = null,
        lastReasoningStartedAt: Long? = null,
        lastReasoningFinishedAt: Long? = null
    )
}

class RoomMessageRepository(
    private val messageDao: MessageDao
) : MessageRepository {
    override fun observeMessages(sessionId: String): Flow<List<Message>> {
        return messageDao.observeMessages(sessionId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun fetchLatest(sessionId: String, limit: Int): List<Message> {
        return messageDao.fetchLatest(sessionId, limit).map { it.toModel() }
    }

    override suspend fun fetchBefore(sessionId: String, before: Long, limit: Int): List<Message> {
        return messageDao.fetchBefore(sessionId, before, limit).map { it.toModel() }
    }

    override suspend fun fetchByStatus(status: MessageStatus): List<Message> {
        return messageDao.fetchByStatus(status.name.lowercase()).map { it.toModel() }
    }

    override suspend fun upsert(message: Message) {
        messageDao.upsert(message.toEntity())
    }

    override suspend fun updateLocalStatus(messageId: String, status: MessageStatus) {
        messageDao.updateLocalStatus(messageId, status.name.lowercase())
    }

    override suspend fun updateRetryMetadata(messageId: String, retryCount: Int, lastAttemptAt: Long?) {
        messageDao.updateRetryMetadata(messageId, retryCount, lastAttemptAt)
    }

    override suspend fun deleteMessagesBefore(before: Long) {
        messageDao.deleteMessagesBefore(before)
    }

    // Version/branch support implementation
    override suspend fun fetchVersions(parentMessageId: String): List<Message> {
        return messageDao.fetchVersions(parentMessageId).map { it.toModel() }
    }

    override suspend fun fetchById(messageId: String): Message? {
        return messageDao.fetchById(messageId)?.toModel()
    }

    override suspend fun fetchMessagesUpToTimestamp(sessionId: String, timestamp: Long): List<Message> {
        return messageDao.fetchMessagesUpToTimestamp(sessionId, timestamp).map { it.toModel() }
    }

    override suspend fun fetchByForkSource(sourceSessionId: String): List<Message> {
        return messageDao.fetchByForkSource(sourceSessionId).map { it.toModel() }
    }

    override suspend fun getMaxVersion(parentMessageId: String): Int {
        return messageDao.getMaxVersion(parentMessageId) ?: 0
    }

    override suspend fun markAsNotLatest(messageId: String) {
        val entity = messageDao.fetchById(messageId)
        entity?.let {
            messageDao.upsert(it.copy(isLatestVersion = false))
        }
    }

    override suspend fun updateMessageVersion(messageId: String, version: Int, previousVersionId: String) {
        val entity = messageDao.fetchById(messageId)
        entity?.let {
            messageDao.upsert(it.copy(version = version, previousVersionId = previousVersionId))
        }
    }

    // Favorite support implementation
    override suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean) {
        messageDao.updateFavoriteStatus(messageId, isFavorite)
    }

    // Generation status support implementation
    override suspend fun updateGenerationStatus(
        messageId: String,
        generationStatus: GenerationStatus?,
        lastToolName: String?,
        lastToolInput: String?,
        lastToolExecutedAt: Long?,
        lastReasoningContent: String?,
        lastReasoningStartedAt: Long?,
        lastReasoningFinishedAt: Long?
    ) {
        val entity = messageDao.fetchById(messageId)
        entity?.let {
            messageDao.upsert(
                it.copy(
                    generationStatus = generationStatus?.toSerializedName(),
                    lastToolName = lastToolName,
                    lastToolInput = lastToolInput,
                    lastToolExecutedAt = lastToolExecutedAt,
                    lastReasoningContent = lastReasoningContent,
                    lastReasoningStartedAt = lastReasoningStartedAt,
                    lastReasoningFinishedAt = lastReasoningFinishedAt
                )
            )
        }
    }
}

private fun MessageEntity.toModel(): Message {
    return Message(
        id = id,
        sessionId = sessionId,
        content = content,
        senderId = senderId,
        timestamp = timestamp,
        type = type.toMessageType(),
        status = localStatus.toMessageStatus(),
        encryptedPayload = encryptedPayload,
        flakeId = flakeId,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt,
        expiresAt = expiresAt,
        translatedContent = translatedContent,
        translationLanguage = translationLanguage,
        parentMessageId = parentMessageId,
        version = version,
        isLatestVersion = isLatestVersion,
        previousVersionId = previousVersionId,
        forkSourceSessionId = forkSourceSessionId,
        forkSourceMessageId = forkSourceMessageId,
        isFavorite = isFavorite,
        generationStatus = generationStatus?.toGenerationStatus(),
        lastToolName = lastToolName,
        lastToolInput = lastToolInput,
        lastToolExecutedAt = lastToolExecutedAt,
        lastReasoningContent = lastReasoningContent,
        lastReasoningStartedAt = lastReasoningStartedAt,
        lastReasoningFinishedAt = lastReasoningFinishedAt
    )
}

private fun Message.toEntity(): MessageEntity {
    return MessageEntity(
        id = id,
        sessionId = sessionId,
        content = content,
        senderId = senderId,
        timestamp = timestamp,
        type = type.serialValue(),
        localStatus = status.name.lowercase(),
        encryptedPayload = encryptedPayload,
        flakeId = flakeId,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt,
        expiresAt = expiresAt,
        translatedContent = translatedContent,
        translationLanguage = translationLanguage,
        parentMessageId = parentMessageId,
        version = version,
        isLatestVersion = isLatestVersion,
        previousVersionId = previousVersionId,
        forkSourceSessionId = forkSourceSessionId,
        forkSourceMessageId = forkSourceMessageId,
        isFavorite = isFavorite,
        generationStatus = generationStatus?.toSerializedName(),
        lastToolName = lastToolName,
        lastToolInput = lastToolInput,
        lastToolExecutedAt = lastToolExecutedAt,
        lastReasoningContent = lastReasoningContent,
        lastReasoningStartedAt = lastReasoningStartedAt,
        lastReasoningFinishedAt = lastReasoningFinishedAt
    )
}

private fun String.toMessageStatus(): MessageStatus {
    return when (lowercase()) {
        "sent" -> MessageStatus.SENT
        "delivered" -> MessageStatus.DELIVERED
        "read" -> MessageStatus.READ
        "failed" -> MessageStatus.FAILED
        "cancelled" -> MessageStatus.CANCELLED
        "translating" -> MessageStatus.TRANSLATING
        else -> MessageStatus.PENDING
    }
}

private fun String.toMessageType(): MessageType {
    return when (lowercase()) {
        "image" -> MessageType.IMAGE
        "file" -> MessageType.FILE
        else -> MessageType.TEXT
    }
}

private fun MessageType.serialValue(): String {
    return when (this) {
        MessageType.TEXT -> "text"
        MessageType.IMAGE -> "image"
        MessageType.FILE -> "file"
    }
}

private fun String.toGenerationStatus(): GenerationStatus? {
    return when {
        startsWith("toolExecuting:") -> {
            val parts = removePrefix("toolExecuting:").split("|")
            GenerationStatus.ToolExecuting(
                toolName = parts.getOrNull(0) ?: "",
                toolInput = parts.getOrNull(1),
                startedAt = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
            )
        }
        startsWith("reasoning:") -> {
            val parts = removePrefix("reasoning:").split("|")
            GenerationStatus.Reasoning(
                content = parts.getOrNull(0) ?: "",
                startedAt = parts.getOrNull(1)?.toLongOrNull() ?: System.currentTimeMillis(),
                finishedAt = parts.getOrNull(2)?.toLongOrNull()
            )
        }
        startsWith("writing:") -> GenerationStatus.Writing(
            content = removePrefix("writing:"),
            startedAt = System.currentTimeMillis()
        )
        startsWith("completed") -> GenerationStatus.Completed
        startsWith("failed:") -> {
            val error = removePrefix("failed:")
            GenerationStatus.Failed(error = error)
        }
        else -> null
    }
}

private fun GenerationStatus.toSerializedName(): String {
    return when (this) {
        is GenerationStatus.ToolExecuting -> "toolExecuting:${toolName}|${toolInput ?: ""}|${startedAt}"
        is GenerationStatus.Reasoning -> "reasoning:${content}|${startedAt}|${finishedAt?.toString() ?: ""}"
        is GenerationStatus.Writing -> "writing:${content}"
        is GenerationStatus.Completed -> "completed"
        is GenerationStatus.Failed -> "failed:${error}"
    }
}