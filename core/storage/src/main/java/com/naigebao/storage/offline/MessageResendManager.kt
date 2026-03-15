package com.naigebao.storage.offline

import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageStatus
import com.naigebao.storage.repository.MessageRepository

class MessageResendManager(
    private val messageRepository: MessageRepository,
    private val sender: suspend (Message) -> Boolean,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES
) {
    suspend fun resendPending(): Int {
        val pending = messageRepository.fetchByStatus(MessageStatus.PENDING) +
            messageRepository.fetchByStatus(MessageStatus.FAILED)
        var sentCount = 0
        for (message in pending) {
            val nextRetry = message.retryCount + 1
            if (nextRetry > maxRetries) {
                continue
            }
            val success = sender(message)
            val updated = if (success) {
                sentCount += 1
                message.copy(status = MessageStatus.SENT, retryCount = nextRetry, lastAttemptAt = now())
            } else {
                message.copy(
                    status = MessageStatus.FAILED,
                    retryCount = nextRetry,
                    lastAttemptAt = now()
                )
            }
            messageRepository.upsert(updated)
            messageRepository.updateRetryMetadata(updated.id, updated.retryCount, updated.lastAttemptAt)
        }
        return sentCount
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        const val DEFAULT_MAX_RETRIES = 5
    }
}
