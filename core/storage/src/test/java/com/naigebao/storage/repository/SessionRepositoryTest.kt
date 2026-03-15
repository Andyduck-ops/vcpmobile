package com.naigebao.storage.repository

import com.naigebao.storage.database.dao.SessionDao
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.database.entity.SessionWithMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun `upsert delegates to dao`() = runTest {
        val dao = FakeSessionDao()
        val repository = RoomSessionRepository(dao)
        val entity = SessionEntity(
            id = "session-1",
            type = "private",
            title = "General",
            updatedAt = 1L
        )

        repository.upsert(entity)

        assertEquals(entity, dao.lastUpserted)
    }

    private class FakeSessionDao : SessionDao {
        val sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
        var lastUpserted: SessionEntity? = null

        override fun observeSessions(): Flow<List<SessionEntity>> = sessions

        override fun searchSessions(query: String): Flow<List<SessionEntity>> {
            return MutableStateFlow(
                sessions.value.filter {
                    it.title.contains(query, ignoreCase = true)
                }
            )
        }

        override fun observeSessionWithMessages(sessionId: String): Flow<SessionWithMessages?> {
            return MutableStateFlow(null)
        }

        override suspend fun getSessionById(sessionId: String): SessionEntity? {
            return sessions.value.firstOrNull { it.id == sessionId }
        }

        override suspend fun upsert(session: SessionEntity) {
            lastUpserted = session
            sessions.value = listOf(session)
        }

        override suspend fun deleteById(sessionId: String) {
            sessions.value = sessions.value.filterNot { it.id == sessionId }
        }

        override suspend fun fetchSessions(): List<SessionEntity> {
            return sessions.value
        }

        override suspend fun searchSessionsSync(query: String): List<SessionEntity> {
            return sessions.value.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }

        override suspend fun updatePinStatus(sessionId: String, isPinned: Boolean) {
            sessions.value = sessions.value.map {
                if (it.id == sessionId) it.copy(isPinned = isPinned) else it
            }
        }
    }
}
