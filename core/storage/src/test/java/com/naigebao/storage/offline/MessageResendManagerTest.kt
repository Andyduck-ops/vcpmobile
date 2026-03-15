package com.naigebao.storage.offline

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.storage.repository.MessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageResendManagerTest {
    @Test
    fun resendPendingUpdatesStatus() = runTest {
        val repository = FakeMessageRepository()
        val message = Message(
            id = "1",
            sessionId = "s1",
            content = "hello",
            senderId = "me",
            timestamp = 1L,
            type = MessageType.TEXT,
            status = MessageStatus.FAILED
        )
        repository.upsert(message)

        val manager = MessageResendManager(
            messageRepository = repository,
            sender = { true }
        )
        val sent = manager.resendPending()

        assertEquals(1, sent)
        val updated = repository.items.first { it.id == "1" }
        assertEquals(MessageStatus.SENT, updated.status)
        assertEquals(1, updated.retryCount)
    }

    private class FakeMessageRepository : MessageRepository {
        val items = mutableListOf<Message>()

        override fun observeMessages(sessionId: String) = throw UnsupportedOperationException()

        override suspend fun fetchLatest(sessionId: String, limit: Int) = emptyList<Message>()

        override suspend fun fetchBefore(sessionId: String, before: Long, limit: Int) = emptyList<Message>()

        override suspend fun fetchByStatus(status: MessageStatus): List<Message> {
            return items.filter { it.status == status }
        }

        override suspend fun upsert(message: Message) {
            items.removeAll { it.id == message.id }
            items.add(message)
        }

        override suspend fun updateLocalStatus(messageId: String, status: MessageStatus) = Unit

        override suspend fun updateRetryMetadata(messageId: String, retryCount: Int, lastAttemptAt: Long?) = Unit

        override suspend fun deleteMessagesBefore(before: Long) = Unit

        override suspend fun fetchVersions(parentMessageId: String): List<Message> {
            return items.filter { it.parentMessageId == parentMessageId }
        }

        override suspend fun fetchById(messageId: String): Message? {
            return items.firstOrNull { it.id == messageId }
        }

        override suspend fun fetchMessagesUpToTimestamp(sessionId: String, timestamp: Long): List<Message> {
            return items.filter { it.sessionId == sessionId && it.timestamp <= timestamp }
        }

        override suspend fun fetchByForkSource(sourceSessionId: String): List<Message> {
            return items.filter { it.forkSourceSessionId == sourceSessionId }
        }

        override suspend fun getMaxVersion(parentMessageId: String): Int {
            return items
                .filter { it.parentMessageId == parentMessageId }
                .maxOfOrNull { it.version }
                ?: 0
        }

        override suspend fun markAsNotLatest(messageId: String) = Unit

        override suspend fun updateMessageVersion(messageId: String, version: Int, previousVersionId: String) = Unit

        override suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean) = Unit

        override suspend fun updateGenerationStatus(
            messageId: String,
            generationStatus: com.naigebao.model.message.GenerationStatus?,
            lastToolName: String?,
            lastToolInput: String?,
            lastToolExecutedAt: Long?,
            lastReasoningContent: String?,
            lastReasoningStartedAt: Long?,
            lastReasoningFinishedAt: Long?
        ) = Unit
    }
}
