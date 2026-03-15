package com.naigebao.chat.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naigebao.chat.tts.TTSManager
import com.naigebao.model.message.GenerationStatus
import com.naigebao.model.message.Message
import com.naigebao.network.chat.ChatService
import com.naigebao.network.chat.OutgoingAttachment
import com.naigebao.network.websocket.WebSocketManager
import com.naigebao.storage.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

enum class AiModel(val displayName: String, val id: String) {
    GPT4("GPT-4", "gpt-4"),
    GPT35("GPT-3.5", "gpt-3.5-turbo"),
    CLAUDE("Claude", "claude-3-opus"),
    GEMINI("Gemini", "gemini-pro")
}

data class Attachment(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)

data class MessageVersionSummary(
    val chainRootId: String,
    val currentIndex: Int,
    val totalCount: Int
)

data class ChatUiState(
    val sessionId: String,
    val messages: List<Message> = emptyList(),
    val totalCount: Int = 0,
    val canLoadMore: Boolean = false,
    val input: String = "",
    val isLoadingMore: Boolean = false,
    val isGenerating: Boolean = false,
    val isTranslating: Boolean = false,
    val isOtherTyping: Boolean = false,
    val activeRequestId: String? = null,
    val currentGenerationStatus: GenerationStatus? = null,
    val ttsPlaybackState: TTSManager.PlaybackState = TTSManager.PlaybackState.IDLE,
    val selectedModel: AiModel = AiModel.GPT4,
    val availableModels: List<AiModel> = AiModel.entries,
    val attachments: List<Attachment> = emptyList(),
    val selectedVersionIds: Map<String, String> = emptyMap(),
    val versionSummaries: Map<String, MessageVersionSummary> = emptyMap()
)

class ChatViewModel(
    private val sessionId: String,
    private val chatService: ChatService,
    private val messageRepository: MessageRepository? = null,
    private val webSocketManager: WebSocketManager? = null,
    private val context: Context? = null
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState(sessionId = sessionId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var ttsManager: TTSManager? = null
    private var pageSize = DEFAULT_PAGE_SIZE
    private var allMessages: List<Message> = emptyList()

    init {
        context?.let {
            ttsManager = TTSManager(it)
            viewModelScope.launch {
                ttsManager?.playbackState?.collect { ttsState ->
                    _state.value = _state.value.copy(ttsPlaybackState = ttsState)
                }
            }
        }

        viewModelScope.launch {
            chatService.observeMessages(sessionId).collect { observedMessages ->
                allMessages = observedMessages
                refreshVisibleMessages()
            }
        }

        observeGenerationStatus()
        observeTypingStatus()
    }

    private fun observeTypingStatus() {
        webSocketManager?.let { wsManager ->
            viewModelScope.launch {
                wsManager.events.collect { event ->
                    when (event.type) {
                        "typing_start" -> {
                            _state.value = _state.value.copy(isOtherTyping = true)
                        }
                        "typing_stop", "message_done", "content_update" -> {
                            _state.value = _state.value.copy(isOtherTyping = false)
                        }
                    }
                }
            }
        }
    }

    private fun observeGenerationStatus() {
        webSocketManager?.let { wsManager ->
            viewModelScope.launch {
                wsManager.events.collect { event ->
                    when (event.type) {
                        "generation_start", "tool_call", "tool_start" -> {
                            val toolName = event.payload["toolName"]?.jsonPrimitive?.content
                            val toolInput = event.payload["input"]?.jsonPrimitive?.content
                            if (toolName != null) {
                                _state.value = _state.value.copy(
                                    isGenerating = true,
                                    currentGenerationStatus = GenerationStatus.ToolExecuting(
                                        toolName = toolName,
                                        toolInput = toolInput
                                    )
                                )
                                updateLastMessageWithTool(toolName, toolInput)
                            }
                        }
                        "reasoning_start", "reasoning_update" -> {
                            val content = event.payload["content"]?.jsonPrimitive?.content
                            if (content != null) {
                                _state.value = _state.value.copy(
                                    currentGenerationStatus = GenerationStatus.Reasoning(
                                        content = content,
                                        startedAt = (_state.value.currentGenerationStatus as? GenerationStatus.Reasoning)?.startedAt
                                            ?: System.currentTimeMillis()
                                    )
                                )
                                updateLastMessageWithReasoning(content)
                            }
                        }
                        "content_update", "chunk" -> {
                            val content = event.payload["content"]?.jsonPrimitive?.content
                            if (content != null) {
                                _state.value = _state.value.copy(
                                    currentGenerationStatus = GenerationStatus.Writing(content = content)
                                )
                            }
                        }
                        "generation_complete", "message_done" -> {
                            _state.value = _state.value.copy(
                                isGenerating = false,
                                isOtherTyping = false,
                                currentGenerationStatus = GenerationStatus.Completed
                            )
                            viewModelScope.launch {
                                val lastAiMessageId = getLastAiMessageId()
                                if (lastAiMessageId != null) {
                                    messageRepository?.updateGenerationStatus(
                                        messageId = lastAiMessageId,
                                        generationStatus = GenerationStatus.Completed,
                                        lastReasoningFinishedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                        "generation_error", "error" -> {
                            val error = event.payload["error"]?.jsonPrimitive?.content ?: "Unknown error"
                            _state.value = _state.value.copy(
                                isGenerating = false,
                                isOtherTyping = false,
                                currentGenerationStatus = GenerationStatus.Failed(error = error)
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun getLastAiMessageId(): String? {
        return allMessages.lastOrNull { it.senderId != "me" }?.id
    }

    private suspend fun updateLastMessageWithTool(toolName: String, toolInput: String?) {
        val messageId = getLastAiMessageId() ?: return
        messageRepository?.updateGenerationStatus(
            messageId = messageId,
            generationStatus = GenerationStatus.ToolExecuting(toolName, toolInput),
            lastToolName = toolName,
            lastToolInput = toolInput,
            lastToolExecutedAt = null
        )
    }

    private suspend fun updateLastMessageWithReasoning(content: String) {
        val messageId = getLastAiMessageId() ?: return
        val startedAt = (_state.value.currentGenerationStatus as? GenerationStatus.Reasoning)?.startedAt
            ?: System.currentTimeMillis()

        messageRepository?.updateGenerationStatus(
            messageId = messageId,
            generationStatus = GenerationStatus.Reasoning(content, startedAt),
            lastReasoningContent = content,
            lastReasoningStartedAt = startedAt,
            lastReasoningFinishedAt = null
        )
    }

    private fun refreshVisibleMessages() {
        val (visibleMessages, summaries) = buildVisibleMessages(
            allMessages = allMessages,
            selectedVersionIds = _state.value.selectedVersionIds,
            pageSize = pageSize
        )
        _state.value = _state.value.copy(
            messages = visibleMessages,
            totalCount = visibleMessages.size,
            canLoadMore = allMessages.isNotEmpty() && visibleMessages.size < buildLogicalMessageOrder(allMessages).size,
            isLoadingMore = false,
            versionSummaries = summaries
        )
    }

    private fun buildVisibleMessages(
        allMessages: List<Message>,
        selectedVersionIds: Map<String, String>,
        pageSize: Int
    ): Pair<List<Message>, Map<String, MessageVersionSummary>> {
        if (allMessages.isEmpty()) {
            return emptyList<Message>() to emptyMap()
        }

        data class DisplayEntry(
            val message: Message,
            val anchorTimestamp: Long,
            val summary: MessageVersionSummary?
        )

        val entries = buildLogicalMessageOrder(allMessages).map { versions ->
            val rootId = chainRootId(versions.first())
            val selectedMessage = versions.firstOrNull { it.id == selectedVersionIds[rootId] }
                ?: versions.maxWith(compareBy<Message>({ it.version }, { it.timestamp }))
            val selectedIndex = versions.indexOfFirst { it.id == selectedMessage.id } + 1
            val anchorTimestamp = versions.firstOrNull { it.id == rootId }?.timestamp
                ?: versions.minOf { it.timestamp }
            val summary = if (versions.size > 1) {
                MessageVersionSummary(
                    chainRootId = rootId,
                    currentIndex = selectedIndex,
                    totalCount = versions.size
                )
            } else {
                null
            }
            DisplayEntry(
                message = selectedMessage,
                anchorTimestamp = anchorTimestamp,
                summary = summary
            )
        }.sortedBy { it.anchorTimestamp }

        val visibleEntries = entries.takeLast(pageSize)
        val visibleMessages = visibleEntries.map { it.message }
        val summaries = visibleEntries
            .mapNotNull { entry -> entry.summary?.let { entry.message.id to it } }
            .toMap()

        return visibleMessages to summaries
    }

    private fun buildLogicalMessageOrder(allMessages: List<Message>): List<List<Message>> {
        return allMessages
            .groupBy(::chainRootId)
            .values
            .map { versions ->
                versions.sortedWith(compareBy<Message>({ it.version }, { it.timestamp }))
            }
    }

    private fun chainRootId(message: Message): String {
        return message.parentMessageId ?: message.id
    }

    private fun versionsForRoot(rootId: String): List<Message> {
        return allMessages
            .filter { chainRootId(it) == rootId }
            .sortedWith(compareBy<Message>({ it.version }, { it.timestamp }))
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(input = value)
    }

    fun loadMore() {
        if (_state.value.isLoadingMore || !_state.value.canLoadMore) return
        _state.value = _state.value.copy(isLoadingMore = true)
        pageSize += DEFAULT_PAGE_SIZE
        refreshVisibleMessages()
    }

    fun sendMessage() {
        val text = _state.value.input.trim()
        if (text.isEmpty() && _state.value.attachments.isEmpty()) return
        viewModelScope.launch {
            val currentState = _state.value
            chatService.sendMessage(
                sessionId = sessionId,
                content = text,
                modelId = currentState.selectedModel.id,
                attachments = currentState.attachments.map { attachment ->
                    OutgoingAttachment(
                        uri = attachment.uri.toString(),
                        name = attachment.name,
                        mimeType = attachment.mimeType,
                        size = attachment.size
                    )
                }
            )
            _state.value = _state.value.copy(
                input = "",
                attachments = emptyList(),
                isOtherTyping = false
            )
        }
    }

    fun selectModel(model: AiModel) {
        _state.value = _state.value.copy(selectedModel = model)
    }

    fun addAttachment(uri: Uri) {
        context?.let { ctx ->
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers do not grant persistable permissions; keep transient access.
            }
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else uri.lastPathSegment ?: "file"
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val mimeType = ctx.contentResolver.getType(uri) ?: "application/octet-stream"

                    val attachment = Attachment(uri = uri, name = name, mimeType = mimeType, size = size)
                    _state.value = _state.value.copy(attachments = _state.value.attachments + attachment)
                }
            }
        }
    }

    fun removeAttachment(attachment: Attachment) {
        _state.value = _state.value.copy(attachments = _state.value.attachments - attachment)
    }

    fun clearAttachments() {
        _state.value = _state.value.copy(attachments = emptyList())
    }

    fun editMessage(messageId: String, newContent: String) {
        viewModelScope.launch { chatService.editMessage(messageId, newContent) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { chatService.deleteMessage(messageId) }
    }

    fun toggleFavorite(messageId: String) {
        viewModelScope.launch { chatService.toggleMessageFavorite(messageId) }
    }

    fun copyMessage(messageId: String): String? {
        return _state.value.messages.find { it.id == messageId }?.content
    }

    fun regenerateMessage(messageId: String) {
        viewModelScope.launch { chatService.regenerateAtMessage(messageId) }
    }

    fun cycleMessageVersion(messageId: String) {
        val summary = _state.value.versionSummaries[messageId] ?: return
        val versions = versionsForRoot(summary.chainRootId)
        if (versions.size < 2) return

        val currentSelectedId = _state.value.selectedVersionIds[summary.chainRootId] ?: messageId
        val currentIndex = versions.indexOfFirst { it.id == currentSelectedId }.coerceAtLeast(0)
        val nextIndex = (currentIndex + 1) % versions.size
        val nextSelections = _state.value.selectedVersionIds.toMutableMap()
        nextSelections[summary.chainRootId] = versions[nextIndex].id
        _state.value = _state.value.copy(selectedVersionIds = nextSelections)
        refreshVisibleMessages()
    }

    fun forkFromMessage(messageId: String, onForked: (String) -> Unit = {}) {
        viewModelScope.launch {
            val newSessionId = chatService.forkConversationAtMessage(messageId)
            if (newSessionId != null) {
                onForked(newSessionId)
            }
        }
    }

    fun stopGeneration() {
        viewModelScope.launch {
            chatService.stopGeneration(sessionId, _state.value.activeRequestId)
            _state.value = _state.value.copy(
                isGenerating = false,
                activeRequestId = null,
                currentGenerationStatus = GenerationStatus.Completed
            )
        }
    }

    fun translateMessage(messageId: String, targetLanguage: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTranslating = true)
            try {
                chatService.translateMessage(messageId, targetLanguage)
            } finally {
                _state.value = _state.value.copy(isTranslating = false)
            }
        }
    }

    fun getTTSManager(): TTSManager? = ttsManager

    fun playTTS(text: String, messageId: String? = null) {
        ttsManager?.play(text, messageId)
    }

    fun pauseTTS() {
        ttsManager?.pause()
    }

    fun resumeTTS() {
        ttsManager?.resume()
    }

    fun stopTTS() {
        ttsManager?.stop()
    }

    fun setTTSRate(rate: Float) {
        ttsManager?.setSpeechRate(rate)
    }

    fun setTTSPitch(pitch: Float) {
        ttsManager?.setPitch(pitch)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.release()
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 30
    }
}
