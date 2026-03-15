package com.naigebao.network.chat

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.model.message.SyncEvent
import com.naigebao.network.websocket.WebSocketManager
import com.naigebao.storage.repository.MessageRepository
import com.naigebao.storage.repository.SessionRepository
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.common.FlakeIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class OutgoingAttachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val size: Long
)

/**
 * Chat service interface for message operations
 * Provides a unified API for chat functionality including:
 * - Send, edit, delete messages
 * - Message regeneration (with version support)
 * - Fork conversation (branch support)
 * - Stop generation
 * - P1 features: translate, favorite, pin
 */
interface ChatService {
    /**
     * Send a new message in the session
     * @return The created message
     */
    suspend fun sendMessage(
        sessionId: String,
        content: String,
        modelId: String? = null,
        attachments: List<OutgoingAttachment> = emptyList()
    ): Message

    /**
     * Edit an existing message
     * @param messageId The message ID to edit
     * @param newContent New content
     * @return Updated message
     */
    suspend fun editMessage(messageId: String, newContent: String): Message?

    /**
     * Delete a message
     * @param messageId The message ID to delete
     * @return true if deleted successfully
     */
    suspend fun deleteMessage(messageId: String): Boolean

    /**
     * Regenerate a response message (simple version without version tracking)
     * @param messageId The message to regenerate
     * @return New message or null
     */
    suspend fun regenerateMessage(messageId: String): Message?

    /**
     * Regenerate at a specific message with version tracking
     * Marks original message as non-latest and creates a new version
     * @param messageId The message to regenerate
     * @return New version message or null
     */
    suspend fun regenerateAtMessage(messageId: String): Message?

    /**
     * Fork from a specific message to create a new branch (simple version)
     * @param messageId The message to fork from
     * @return New session ID for the fork
     */
    suspend fun forkFromMessage(messageId: String): String?

    /**
     * Fork conversation at a specific message with full branch tracking
     * Creates a new session and copies all messages up to the fork point
     * @param messageId The message to fork from
     * @param newSessionTitle Optional title for the new session
     * @return New session ID for the fork
     */
    suspend fun forkConversationAtMessage(messageId: String, newSessionTitle: String? = null): String?

    /**
     * Stop ongoing generation for a session
     * @param sessionId The session to stop
     * @param requestId Optional request ID to cancel specific request
     */
    suspend fun stopGeneration(sessionId: String, requestId: String? = null)

    /**
     * Translate a message to target language
     * @param messageId The message ID to translate
     * @param targetLanguage Target language code (e.g., "en", "zh", "ja")
     * @return Translated message or null if failed
     */
    suspend fun translateMessage(messageId: String, targetLanguage: String): Message?

    /**
     * Get message by ID
     * @param messageId The message ID
     * @return Message or null if not found
     */
    suspend fun getMessage(messageId: String): Message?

    /**
     * Get all versions of a message (for branching display)
     * @param parentMessageId The parent message ID to get versions for
     * @return List of message versions
     */
    suspend fun getMessageVersions(parentMessageId: String): List<Message>

    /**
     * Observe messages for a session
     * @param sessionId The session ID
     * @return Flow of message list
     */
    fun observeMessages(sessionId: String): Flow<List<Message>>

    /**
     * Get messages for a session
     * @param sessionId The session ID
     * @param limit Max messages to fetch
     * @return List of messages
     */
    suspend fun getMessages(sessionId: String, limit: Int): List<Message>

    /**
     * Toggle message favorite status
     * @param messageId The message ID
     * @return Updated message or null if not found
     */
    suspend fun toggleMessageFavorite(messageId: String): Message?

    /**
     * Toggle session pin status
     * @param sessionId The session ID
     * @return Updated session or null if not found
     */
    suspend fun togglePinStatus(sessionId: String): SessionEntity?

    /**
     * Generate title for session
     * Uses first user message or AI generation
     * @param sessionId The session ID (optional, uses current session if null)
     * @return Generated title or null
     */
    suspend fun generateTitle(sessionId: String? = null): String?
}
