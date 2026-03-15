package com.naigebao.storage.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val lastMessageId: String? = null,
    val lastMessagePreview: String? = null,
    val unreadCount: Int = 0,
    val updatedAt: Long,
    val lastSyncedAt: Long? = null,
    // Fork support: source session when this session was forked
    val forkSourceSessionId: String? = null,
    val forkSourceMessageId: String? = null,
    // Pin support
    val isPinned: Boolean = false
)
