package com.naigebao.network.chat

import com.naigebao.model.message.GenerationStatus
import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.model.message.MessageType
import com.naigebao.model.message.SyncEvent
import com.naigebao.network.websocket.WebSocketManager
import com.naigebao.storage.database.entity.SessionEntity
import com.naigebao.storage.database.entity.SessionWithMessages
import com.naigebao.storage.repository.MessageRepository
import com.naigebao.storage.repository.SessionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatServiceImplTest {

    @Test
    fun `editMessage updates message found by id`() = runTest {
        val messageRepository = FakeMessageRepository()
        val sessionRepository = FakeSessionRepository()
        val webSocketManager = fakeWebSocketManager()
        val original = message(id = "message-1", sessionId = "session-1", content = "before")
        messageRepository.upsert(original)

        val service = ChatServiceImpl(
            messageRepository = messageRepository,
            sessionRepository = sessionRepository,
            webSocketManager = webSocketManager
        )

        val updated = service.editMessage("message-1", "after")

        assertNotNull(updated)
        assertEquals("after", messageRepository.fetchById("message-1")?.content)
    }

    @Test
    fun `regenerateAtMessage keeps versions on the same root chain`() = runTest {
        val messageRepository = FakeMessageRepository()
        val sessionRepository = FakeSessionRepository()
        val webSocketManager = fakeWebSocketManager()
        val root = message(id = "root", sessionId = "session-1", content = "root reply", senderId = "assistant")
        messageRepository.upsert(root)

        val service = ChatServiceImpl(
            messageRepository = messageRepository,
            sessionRepository = sessionRepository,
            webSocketManager = webSocketManager
        )

        val version2 = service.regenerateAtMessage("root")
        val version3 = service.regenerateAtMessage(version2!!.id)
        val versions = service.getMessageVersions(version3!!.id)

        assertEquals("root", version2.parentMessageId)
        assertEquals("root", version3.parentMessageId)
        assertEquals(version2.id, version3.previousVersionId)
        assertEquals(listOf(1, 2, 3), versions.map { it.version })
        assertEquals(3, versions.size)
    }

    @Test
    fun `sendMessage preserves existing session metadata when updating preview`() = runTest {
        val messageRepository = FakeMessageRepository()
        val sessionRepository = FakeSessionRepository()
        val webSocketManager = fakeWebSocketManager()
        sessionRepository.upsert(
            SessionEntity(
                id = "session-1",
                type = "private",
                title = "General",
                lastMessagePreview = "old",
                updatedAt = 1L
            )
        )

        val service = ChatServiceImpl(
            messageRepository = messageRepository,
            sessionRepository = sessionRepository,
            webSocketManager = webSocketManager
        )

        service.sendMessage("session-1", "new preview")

        val updatedSession = sessionRepository.getSessionById("session-1")
        assertEquals("private", updatedSession?.type)
        assertEquals("General", updatedSession?.title)
        assertEquals("new preview", updatedSession?.lastMessagePreview)
    }

    @Test
    fun `sendMessage includes selected model and attachment payloads`() = runTest {
        val messageRepository = FakeMessageRepository()
        val sessionRepository = FakeSessionRepository()
        val sentEvents = mutableListOf<SyncEvent>()
        val webSocketManager = mockk<WebSocketManager>(relaxed = true) {
            every { send(any()) } answers {
                sentEvents += invocation.args[0] as SyncEvent
                true
            }
        }

        val service = ChatServiceImpl(
            messageRepository = messageRepository,
            sessionRepository = sessionRepository,
            webSocketManager = webSocketManager
        )

        service.sendMessage(
            sessionId = "session-1",
            content = "hello",
            modelId = "gpt-4",
            attachments = listOf(
                OutgoingAttachment(
                    uri = "content://attachments/image.png",
                    name = "image.png",
                    mimeType = "image/png",
                    size = 42L
                )
            )
        )

        assertEquals(2, sentEvents.size)
        assertEquals("gpt-4", sentEvents[0].payload["modelId"]?.jsonPrimitive?.content)
        assertEquals("text", sentEvents[0].payload["type"]?.jsonPrimitive?.content)
        assertEquals("image", sentEvents[1].payload["type"]?.jsonPrimitive?.content)
        assertEquals("image.png", sentEvents[1].payload["attachmentName"]?.jsonPrimitive?.content)
        assertEquals("content://attachments/image.png", sentEvents[1].payload["attachmentUri"]?.jsonPrimitive?.content)
        assertEquals(2, messageRepository.fetchLatest("session-1", 10).size)
    }

    private fun fakeWebSocketManager(): WebSocketManager {
        return mockk(relaxed = true) {
            every { send(any()) } returns true
        }
    }

    private fun message(
        id: String,
        sessionId: String,
        content: String,
        senderId: String = "me"
    ): Message {
        return Message(
            id = id,
            sessionId = sessionId,
            content = content,
            senderId = senderId,
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT,
            status = MessageStatus.SENT
        )
    }

    private class FakeMessageRepository : MessageRepository {
        private val messages = linkedMapOf<String, Message>()
        private val flow = MutableStateFlow<List<Message>>(emptyList())

        override fun observeMessages(sessionId: String): Flow<List<Message>> {
            return flow.map { items -> items.filter { it.sessionId == sessionId }.sortedBy { it.timestamp } }
        }

        override suspend fun fetchLatest(sessionId: String, limit: Int): List<Message> {
            return messages.values
                .filter { it.sessionId == sessionId }
                .sortedByDescending { it.timestamp }
                .take(limit)
        }

        override suspend fun fetchBefore(sessionId: String, before: Long, limit: Int): List<Message> {
            return messages.values
                .filter { it.sessionId == sessionId && it.timestamp < before }
                .sortedByDescending { it.timestamp }
                .take(limit)
        }

        override suspend fun fetchByStatus(status: MessageStatus): List<Message> {
            return messages.values.filter { it.status == status }
        }

        override suspend fun upsert(message: Message) {
            messages[message.id] = message
            flow.value = messages.values.sortedBy { it.timestamp }
        }

        override suspend fun updateLocalStatus(messageId: String, status: MessageStatus) {
            val message = messages[messageId] ?: return
            upsert(message.copy(status = status))
        }

        override suspend fun updateRetryMetadata(messageId: String, retryCount: Int, lastAttemptAt: Long?) {
            val message = messages[messageId] ?: return
            upsert(message.copy(retryCount = retryCount, lastAttemptAt = lastAttemptAt))
        }

        override suspend fun deleteMessagesBefore(before: Long) {
            val remaining = messages.values.filter { it.timestamp >= before }
            messages.clear()
            remaining.forEach { messages[it.id] = it }
            flow.value = messages.values.sortedBy { it.timestamp }
        }

        override suspend fun fetchVersions(parentMessageId: String): List<Message> {
            return messages.values
                .filter { it.parentMessageId == parentMessageId }
                .sortedBy { it.version }
        }

        override suspend fun fetchById(messageId: String): Message? {
            return messages[messageId]
        }

        override suspend fun fetchMessagesUpToTimestamp(sessionId: String, timestamp: Long): List<Message> {
            return messages.values
                .filter { it.sessionId == sessionId && it.timestamp <= timestamp }
                .sortedBy { it.timestamp }
        }

        override suspend fun fetchByForkSource(sourceSessionId: String): List<Message> {
            return messages.values.filter { it.forkSourceSessionId == sourceSessionId }
        }

        override suspend fun getMaxVersion(parentMessageId: String): Int {
            return messages.values
                .filter { it.parentMessageId == parentMessageId }
                .maxOfOrNull { it.version }
                ?: 1
        }

        override suspend fun markAsNotLatest(messageId: String) {
            val message = messages[messageId] ?: return
            upsert(message.copy(isLatestVersion = false))
        }

        override suspend fun updateMessageVersion(messageId: String, version: Int, previousVersionId: String) {
            val message = messages[messageId] ?: return
            upsert(message.copy(version = version, previousVersionId = previousVersionId))
        }

        override suspend fun updateFavoriteStatus(messageId: String, isFavorite: Boolean) {
            val message = messages[messageId] ?: return
            upsert(message.copy(isFavorite = isFavorite))
        }

        override suspend fun updateGenerationStatus(
            messageId: String,
            generationStatus: GenerationStatus?,
            lastToolName: String?,
            lastToolInput: String?,
            lastToolExecutedAt: Long?,
            lastReasoningContent: String?,
            lastReasoningStartedAt: Long?,
            lastReasoningFinishedAt: Long?
        ) {
            val message = messages[messageId] ?: return
            upsert(
                message.copy(
                    generationStatus = generationStatus,
                    lastToolName = lastToolName,
                    lastToolInput = lastToolInput,
                    lastToolExecutedAt = lastToolExecutedAt,
                    lastReasoningContent = lastReasoningContent,
                    lastReasoningStartedAt = lastReasoningStartedAt,
                    lastReasoningFinishedAt = lastReasoningFinishedAt
                )
            )
        }
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessions = linkedMapOf<String, SessionEntity>()
        private val flow = MutableStateFlow<List<SessionEntity>>(emptyList())

        override fun observeSessions(): Flow<List<SessionEntity>> = flow

        override fun searchSessions(query: String): Flow<List<SessionEntity>> {
            return flow.map { items ->
                items.filter { it.title.contains(query, ignoreCase = true) }
            }
        }

        override suspend fun getAllSessionsSync(): List<SessionEntity> = sessions.values.toList()

        override suspend fun searchSessionsSync(query: String): List<SessionEntity> {
            return sessions.values.filter { it.title.contains(query, ignoreCase = true) }
        }

        override fun observeSession(sessionId: String): Flow<SessionWithMessages?> {
            return MutableStateFlow(null)
        }

        override suspend fun getSessionById(sessionId: String): SessionEntity? = sessions[sessionId]

        override suspend fun upsert(session: SessionEntity) {
            sessions[session.id] = session
            flow.value = sessions.values.toList()
        }

        override suspend fun deleteById(sessionId: String) {
            sessions.remove(sessionId)
            flow.value = sessions.values.toList()
        }

        override suspend fun updatePinStatus(sessionId: String, isPinned: Boolean) {
            val session = sessions[sessionId] ?: return
            upsert(session.copy(isPinned = isPinned))
        }
    }
}
