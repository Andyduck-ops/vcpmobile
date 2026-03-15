package com.naigebao.storage.offline

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.storage.repository.MessageRepository

class OfflineMessageQueue(
    private val messageRepository: MessageRepository
) {
    suspend fun enqueue(message: Message): Message {
        val queued = message.copy(status = MessageStatus.PENDING)
        messageRepository.upsert(queued)
        return queued
    }

    suspend fun pendingMessages(): List<Message> {
        return messageRepository.fetchByStatus(MessageStatus.PENDING)
    }
}
