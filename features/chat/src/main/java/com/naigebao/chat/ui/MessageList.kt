package com.naigebao.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naigebao.chat.tts.TTSManager
import com.naigebao.chat.viewmodel.MessageVersionSummary
import com.naigebao.model.message.Message

@Composable
fun MessageList(
    messages: List<Message>,
    versionSummaries: Map<String, MessageVersionSummary>,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onEditMessage: ((String, String) -> Unit)? = null,
    onDeleteMessage: ((String) -> Unit)? = null,
    onRegenerateMessage: ((String) -> Unit)? = null,
    onCycleMessageVersion: ((String) -> Unit)? = null,
    onFavoriteMessage: ((String) -> Unit)? = null,
    onForkFromMessage: ((String) -> Unit)? = null,
    onTranslateMessage: ((String) -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    ttsManager: TTSManager? = null,
    isOtherTyping: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (canLoadMore) {
            item(key = "load_more") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = onLoadMore) {
                        Text("Load earlier")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        items(messages, key = { it.id }) { message ->
            val isMe = message.senderId == "me"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
            ) {
                MessageBubble(
                    message = message,
                    isMe = isMe,
                    onEdit = if (isMe && onEditMessage != null) {
                        { newContent -> onEditMessage(message.id, newContent) }
                    } else {
                        null
                    },
                    onDelete = if (isMe && onDeleteMessage != null) {
                        { onDeleteMessage(message.id) }
                    } else {
                        null
                    },
                    onFavorite = onFavoriteMessage?.let { callback -> { callback(message.id) } },
                    onRegenerate = if (!isMe && onRegenerateMessage != null) {
                        { onRegenerateMessage(message.id) }
                    } else {
                        null
                    },
                    onCycleVersion = versionSummaries[message.id]?.let {
                        if (onCycleMessageVersion != null) ({ onCycleMessageVersion(message.id) }) else null
                    },
                    onFork = if (!isMe && onForkFromMessage != null) {
                        { onForkFromMessage(message.id) }
                    } else {
                        null
                    },
                    onTranslate = if (!isMe && onTranslateMessage != null) {
                        { onTranslateMessage(message.id) }
                    } else {
                        null
                    },
                    versionSummary = versionSummaries[message.id],
                    onShowSnackbar = onShowSnackbar,
                    ttsManager = ttsManager
                )
            }
        }

        if (isOtherTyping) {
            item(key = "typing_indicator") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TypingIndicator()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
