package com.naigebao.chat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naigebao.chat.viewmodel.ChatViewModel
import com.naigebao.ui.components.MessageListSkeleton
import com.naigebao.ui.components.SnackbarHost
import com.naigebao.ui.components.SnackbarManager

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    title: String,
    onOpenSession: ((String) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val snackbarManager = remember { SnackbarManager() }
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach(viewModel::addAttachment)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            when {
                state.messages.isEmpty() && state.isGenerating -> {
                    MessageListSkeleton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                }
                else -> {
                    MessageList(
                        messages = state.messages,
                        versionSummaries = state.versionSummaries,
                        canLoadMore = state.canLoadMore,
                        onLoadMore = viewModel::loadMore,
                        onEditMessage = { messageId, newContent -> viewModel.editMessage(messageId, newContent) },
                        onDeleteMessage = viewModel::deleteMessage,
                        onRegenerateMessage = viewModel::regenerateMessage,
                        onCycleMessageVersion = viewModel::cycleMessageVersion,
                        onFavoriteMessage = viewModel::toggleFavorite,
                        onForkFromMessage = { messageId ->
                            viewModel.forkFromMessage(messageId) { newSessionId ->
                                snackbarManager.showInfo("Forked into a new session")
                                onOpenSession?.invoke(newSessionId)
                            }
                        },
                        onTranslateMessage = { messageId -> viewModel.translateMessage(messageId, "en") },
                        onShowSnackbar = { msg -> snackbarManager.showInfo(msg) },
                        ttsManager = viewModel.getTTSManager(),
                        isOtherTyping = state.isOtherTyping,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            MessageInput(
                value = state.input,
                onValueChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                selectedModel = state.selectedModel,
                availableModels = state.availableModels,
                onModelSelected = viewModel::selectModel,
                isGenerating = state.isGenerating,
                onStopGeneration = viewModel::stopGeneration,
                attachments = state.attachments,
                onRequestAttachment = {
                    attachmentPicker.launch(arrayOf("*/*"))
                },
                onRemoveAttachment = viewModel::removeAttachment
            )
        }

        SnackbarHost(
            snackbarMessages = snackbarManager.messages,
            onDismiss = { message -> snackbarManager.dismiss(message) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
