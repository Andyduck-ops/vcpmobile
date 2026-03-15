package com.naigebao.storage.repository

import com.naigebao.storage.database.dao.SessionDao
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.database.entity.SessionWithMessages
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeSessions(): Flow<List<SessionEntity>>

    fun searchSessions(query: String): Flow<List<SessionEntity>>

    suspend fun getAllSessionsSync(): List<SessionEntity>

    suspend fun searchSessionsSync(query: String): List<SessionEntity>

    fun observeSession(sessionId: String): Flow<SessionWithMessages?>

    suspend fun getSessionById(sessionId: String): SessionEntity?

    suspend fun upsert(session: SessionEntity)

    suspend fun deleteById(sessionId: String)

    suspend fun updatePinStatus(sessionId: String, isPinned: Boolean)
}

class RoomSessionRepository(
    private val sessionDao: SessionDao
) : SessionRepository {
    override fun observeSessions(): Flow<List<SessionEntity>> = sessionDao.observeSessions()

    override fun searchSessions(query: String): Flow<List<SessionEntity>> = sessionDao.searchSessions(query)

    override suspend fun getAllSessionsSync(): List<SessionEntity> = sessionDao.fetchSessions()

    override suspend fun searchSessionsSync(query: String): List<SessionEntity> = sessionDao.searchSessionsSync(query)

    override fun observeSession(sessionId: String): Flow<SessionWithMessages?> {
        return sessionDao.observeSessionWithMessages(sessionId)
    }

    override suspend fun getSessionById(sessionId: String): SessionEntity? {
        return sessionDao.getSessionById(sessionId)
    }

    override suspend fun upsert(session: SessionEntity) {
        sessionDao.upsert(session)
    }

    override suspend fun deleteById(sessionId: String) {
        sessionDao.deleteById(sessionId)
    }

    override suspend fun updatePinStatus(sessionId: String, isPinned: Boolean) {
        sessionDao.updatePinStatus(sessionId, isPinned)
    }
}