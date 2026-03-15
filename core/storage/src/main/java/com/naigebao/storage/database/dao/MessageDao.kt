package com.naigebao.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.naigebao.storage.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessages(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun fetchLatest(sessionId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp < :before ORDER BY timestamp DESC LIMIT :limit")
    suspend fun fetchBefore(sessionId: String, before: Long, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE localStatus = :status ORDER BY timestamp ASC")
    suspend fun fetchByStatus(status: String): List<MessageEntity>

    // Version/branch support queries
    @Query("SELECT * FROM messages WHERE parentMessageId = :parentMessageId ORDER BY version DESC")
    suspend fun fetchVersions(parentMessageId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun fetchById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND timestamp <= :timestamp ORDER BY timestamp ASC")
    suspend fun fetchMessagesUpToTimestamp(sessionId: String, timestamp: Long): List<MessageEntity>

    // Fork support queries
    @Query("SELECT * FROM messages WHERE forkSourceSessionId = :sourceSessionId ORDER BY timestamp ASC")
    suspend fun fetchByForkSource(sourceSessionId: String): List<MessageEntity>

    @Query("SELECT MAX(version) FROM messages WHERE parentMessageId = :parentMessageId")
    suspend fun getMaxVersion(parentMessageId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET localStatus = :status WHERE id = :messageId")
    suspend fun updateLocalStatus(messageId: String, status: String)

    @Query("UPDATE messages SET retryCount = :retryCount, lastAttemptAt = :lastAttemptAt WHERE id = :messageId")
    suspend fun updateRetryMetadata(messageId: String, retryCount: Int, lastAttemptAt: Long?)

    // Favorite support
    @Query("UPDATE messages SET isFavorite = :isFavorite WHERE id = :messageId")
    suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId AND isFavorite = 1 ORDER BY timestamp DESC")
    suspend fun fetchFavoritesBySession(sessionId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE timestamp < :before")
    suspend fun deleteMessagesBefore(before: Long)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId AND timestamp < :before")
    suspend fun deleteMessagesBeforeForSession(sessionId: String, before: Long)
}
