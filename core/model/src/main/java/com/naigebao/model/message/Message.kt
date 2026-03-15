package com.naigebao.model.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the generation status of an AI response message
 * Used to display real-time status to users during AI generation
 */
@Serializable
sealed class GenerationStatus {
    /**
     * AI is executing a tool (e.g., searching, calculating)
     * Contains information about the tool being executed
     */
    @Serializable
    @SerialName("toolExecuting")
    data class ToolExecuting(
        val toolName: String,
        val toolInput: String? = null,
        val startedAt: Long = System.currentTimeMillis()
    ) : GenerationStatus()

    /**
     * AI is thinking/reasoning
     * Contains reasoning content that is in progress
     */
    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        val content: String,
        val startedAt: Long = System.currentTimeMillis(),
        val finishedAt: Long? = null
    ) : GenerationStatus()

    /**
     * AI is writing text response
     * This is the final output phase
     */
    @Serializable
    @SerialName("writing")
    data class Writing(
        val content: String,
        val startedAt: Long = System.currentTimeMillis()
    ) : GenerationStatus()

    /**
     * Generation completed successfully
     */
    @Serializable
    @SerialName("completed")
    data object Completed : GenerationStatus()

    /**
     * Generation failed
     */
    @Serializable
    @SerialName("failed")
    data class Failed(
        val error: String,
        val errorCode: String? = null
    ) : GenerationStatus()
}

@Serializable
enum class MessageStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("sent")
    SENT,

    @SerialName("delivered")
    DELIVERED,

    @SerialName("read")
    READ,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,

    @SerialName("translating")
    TRANSLATING
}

@Serializable
enum class MessageType {
    @SerialName("text")
    TEXT,

    @SerialName("image")
    IMAGE,

    @SerialName("file")
    FILE
}

@Serializable
data class Message(
    val id: String,
    val sessionId: String,
    val content: String,
    val senderId: String,
    val timestamp: Long,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.PENDING,
    val encryptedPayload: String? = null,
    val flakeId: String? = null,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val expiresAt: Long? = null,
    // Translation support
    val translatedContent: String? = null,
    val translationLanguage: String? = null,
    // Branch/version support for regenerate and fork features
    val parentMessageId: String? = null,
    val version: Int = 1,
    val isLatestVersion: Boolean = true,
    val previousVersionId: String? = null,
    // Fork support: source session and message when forked
    val forkSourceSessionId: String? = null,
    val forkSourceMessageId: String? = null,
    // Favorite support
    val isFavorite: Boolean = false,
    // Generation status for AI response messages (for UI status display)
    val generationStatus: GenerationStatus? = null,
    // Last tool being executed (for toolExecuting status)
    val lastToolName: String? = null,
    val lastToolInput: String? = null,
    val lastToolExecutedAt: Long? = null,
    // Last reasoning content (for reasoning status)
    val lastReasoningContent: String? = null,
    val lastReasoningStartedAt: Long? = null,
    val lastReasoningFinishedAt: Long? = null
)