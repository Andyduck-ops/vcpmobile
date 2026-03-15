package com.naigebao.storage.cleanup

import com.naigebao.storage.database.dao.MessageDao
import com.naigebao.storage.database.dao.SessionDao
import java.util.concurrent.TimeUnit

class DataCleanupManager(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao
) {
    suspend fun cleanup() {
        val now = System.currentTimeMillis()
        val sessions = sessionDao.fetchSessions()
        for (session in sessions) {
            val retentionMillis = retentionForSession(session.type)
            if (retentionMillis == Long.MAX_VALUE) {
                continue
            }
            val threshold = now - retentionMillis
            messageDao.deleteMessagesBeforeForSession(session.id, threshold)
        }
    }

    private fun retentionForSession(type: String): Long {
        return when (type.lowercase()) {
            "temporary" -> TimeUnit.DAYS.toMillis(7)
            "favorite" -> Long.MAX_VALUE
            else -> TimeUnit.DAYS.toMillis(30)
        }
    }
}
