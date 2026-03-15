package com.naigebao.storage.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["flakeId"], unique = true),
        Index(value = ["sessionId", "timestamp"]),
        Index(value = ["parentMessageId"]),
        Index(value = ["forkSourceSessionId"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val senderId: String,
    val timestamp: Long,
    val type: String,
    val localStatus: String,
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
    val generationStatus: String? = null,
    // Last tool being executed (for toolExecuting status)
    val lastToolName: String? = null,
    val lastToolInput: String? = null,
    val lastToolExecutedAt: Long? = null,
    // Last reasoning content (for reasoning status)
    val lastReasoningContent: String? = null,
    val lastReasoningStartedAt: Long? = null,
    val lastReasoningFinishedAt: Long? = null
)