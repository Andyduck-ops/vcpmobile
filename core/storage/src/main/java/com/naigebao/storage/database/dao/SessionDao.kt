package com.naigebao.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.database.entity.SessionWithMessages
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, updatedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("""
        SELECT * FROM sessions
        WHERE title LIKE '%' || :query || '%'
        OR id IN (
            SELECT sessionId FROM messages WHERE content LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchSessions(query: String): Flow<List<SessionEntity>>

    @Query("""
        SELECT * FROM sessions
        WHERE title LIKE '%' || :query || '%'
        OR id IN (
            SELECT sessionId FROM messages WHERE content LIKE '%' || :query || '%'
        )
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    suspend fun searchSessionsSync(query: String): List<SessionEntity>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSessionWithMessages(sessionId: String): Flow<SessionWithMessages?>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("SELECT * FROM sessions")
    suspend fun fetchSessions(): List<SessionEntity>

    // Pin support
    @Query("UPDATE sessions SET isPinned = :isPinned WHERE id = :sessionId")
    suspend fun updatePinStatus(sessionId: String, isPinned: Boolean)
}