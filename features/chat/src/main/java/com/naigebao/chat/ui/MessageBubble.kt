package com.naigebao.chat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.naigebao.chat.tts.TTSManager
import com.naigebao.chat.viewmodel.MessageVersionSummary
import com.naigebao.model.message.GenerationStatus
import com.naigebao.model.message.Message
import com.naigebao.model.message.MessageType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean,
    onEdit: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onCycleVersion: (() -> Unit)? = null,
    onFork: (() -> Unit)? = null,
    onTranslate: (() -> Unit)? = null,
    versionSummary: MessageVersionSummary? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    ttsManager: TTSManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember(message.id, message.content) { mutableStateOf(message.content) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var isTtsPaused by remember { mutableStateOf(false) }

    val speechRate by ttsManager?.speechRate?.collectAsState() ?: remember { mutableStateOf(1.0f) }
    val pitch by ttsManager?.pitch?.collectAsState() ?: remember { mutableStateOf(1.0f) }

    val displayStatus = remember(message) {
        val lastToolName = message.lastToolName
        val lastReasoningContent = message.lastReasoningContent
        val lastReasoningStartedAt = message.lastReasoningStartedAt
        val lastReasoningFinishedAt = message.lastReasoningFinishedAt
        message.generationStatus ?: when {
            lastToolName != null && message.lastToolExecutedAt == null -> {
                GenerationStatus.ToolExecuting(
                    toolName = lastToolName,
                    toolInput = message.lastToolInput
                )
            }
            lastReasoningContent != null && lastReasoningFinishedAt == null -> {
                GenerationStatus.Reasoning(
                    content = lastReasoningContent,
                    startedAt = lastReasoningStartedAt ?: System.currentTimeMillis()
                )
            }
            !isMe && message.content.isNotBlank() && lastReasoningStartedAt != null &&
                lastReasoningFinishedAt == null -> {
                GenerationStatus.Writing(content = message.content)
            }
            else -> null
        }
    }

    val hasMenuActions = remember(
        isMe,
        message.id,
        message.isFavorite,
        onEdit,
        onDelete,
        onFavorite,
        onRegenerate,
        onCycleVersion,
        onFork,
        onTranslate,
        versionSummary
    ) {
        val hasCopyAction = message.content.isNotBlank() || !message.lastReasoningContent.isNullOrBlank()
        if (isMe) {
            hasCopyAction || onEdit != null || onDelete != null || onFavorite != null
        } else {
            hasCopyAction ||
                onRegenerate != null ||
                (onCycleVersion != null && versionSummary != null) ||
                onFork != null ||
                onTranslate != null ||
                onFavorite != null
        }
    }

    LaunchedEffect(ttsManager) {
        ttsManager?.playbackState?.collect { state ->
            isTtsPlaying = state == TTSManager.PlaybackState.PLAYING
            isTtsPaused = state == TTSManager.PlaybackState.PAUSED
        }
    }

    val background = if (isMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background, shape)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    if (hasMenuActions) {
                        showContextMenu = true
                    }
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isMe && displayStatus != null) {
                GenerationStatusIndicator(
                    status = displayStatus,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (message.isFavorite || versionSummary != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isFavorite) {
                        StatusPill(
                            label = "Favorite",
                            background = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    if (versionSummary != null) {
                        TextButton(
                            onClick = { onCycleVersion?.invoke() },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Version ${versionSummary.currentIndex}/${versionSummary.totalCount}")
                        }
                    }
                }
            }

            when (message.type) {
                MessageType.IMAGE -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Image message",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MessageType.FILE -> {
                    AttachmentCard(
                        title = message.content,
                        subtitle = "File attachment",
                        textColor = textColor
                    )
                }
                MessageType.TEXT -> {
                    Text(
                        text = message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val translatedContent = message.translatedContent
            if (!translatedContent.isNullOrBlank()) {
                TranslationCard(
                    translatedContent = translatedContent,
                    language = message.translationLanguage,
                    textColor = textColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isMe && ttsManager != null && message.type == MessageType.TEXT && message.content.isNotBlank()) {
                    TTSPlaybackButton(
                        isPlaying = isTtsPlaying,
                        isPaused = isTtsPaused,
                        currentRate = speechRate,
                        currentPitch = pitch,
                        onPlay = { ttsManager.play(message.content, message.id) },
                        onPause = { ttsManager.pause() },
                        onStop = { ttsManager.stop() },
                        onResume = { ttsManager.resume() },
                        onRateChange = { value -> ttsManager.setSpeechRate(value) },
                        onPitchChange = { value -> ttsManager.setPitch(value) }
                    )
                }

                if (hasMenuActions) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "...",
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.combinedClickable(
                            onClick = { showContextMenu = true },
                            onLongClick = { showContextMenu = true }
                        )
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            val copyLabel = when (message.type) {
                MessageType.IMAGE -> "Copy Image URI"
                MessageType.FILE -> "Copy File Name"
                MessageType.TEXT -> "Copy Text"
            }
            if (message.content.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(copyLabel) },
                    onClick = {
                        showContextMenu = false
                        copyToClipboard(context, message.content, onShowSnackbar)
                    }
                )
            }
            message.lastReasoningContent?.takeIf { it.isNotBlank() }?.let { reasoningContent ->
                DropdownMenuItem(
                    text = { Text("Copy Reasoning") },
                    onClick = {
                        showContextMenu = false
                        copyToClipboard(context, reasoningContent, onShowSnackbar)
                    }
                )
            }
            if (onFavorite != null) {
                DropdownMenuItem(
                    text = { Text(if (message.isFavorite) "Remove Favorite" else "Favorite") },
                    onClick = {
                        showContextMenu = false
                        onFavorite()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = "Favorite")
                    }
                )
            }
            if (isMe && onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showContextMenu = false
                        editText = message.content
                        showEditDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                )
            }
            if (isMe && onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showContextMenu = false
                        showDeleteDialog = true
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                )
            }
            if (!isMe && onRegenerate != null) {
                DropdownMenuItem(
                    text = { Text("Regenerate") },
                    onClick = {
                        showContextMenu = false
                        onRegenerate()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                    }
                )
            }
            if (!isMe && onCycleVersion != null && versionSummary != null) {
                DropdownMenuItem(
                    text = { Text("Switch Version") },
                    onClick = {
                        showContextMenu = false
                        onCycleVersion()
                    }
                )
            }
            if (!isMe && onFork != null) {
                DropdownMenuItem(
                    text = { Text("Fork From Here") },
                    onClick = {
                        showContextMenu = false
                        onFork()
                    }
                )
            }
            if (!isMe && onTranslate != null) {
                DropdownMenuItem(
                    text = { Text("Translate to English") },
                    onClick = {
                        showContextMenu = false
                        onTranslate()
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank()) {
                            onEdit?.invoke(editText)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
    onShowSnackbar: ((String) -> Unit)?
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
    onShowSnackbar?.invoke("Copied to clipboard")
}

@Composable
private fun StatusPill(
    label: String,
    background: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
private fun AttachmentCard(
    title: String,
    subtitle: String,
    textColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.75f)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
private fun TranslationCard(
    translatedContent: String,
    language: String?,
    textColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = buildString {
                append("Translated")
                if (!language.isNullOrBlank()) {
                    append(" · ")
                    append(language.uppercase())
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.75f)
        )
        Text(
            text = translatedContent,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

@Composable
private fun GenerationStatusIndicator(
    status: GenerationStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_animation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(alpha)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
        )

        val label = when (status) {
            is GenerationStatus.ToolExecuting -> "Executing: ${status.toolName}"
            is GenerationStatus.Reasoning -> "Reasoning..."
            is GenerationStatus.Writing -> "Writing..."
            is GenerationStatus.Completed -> "Completed"
            is GenerationStatus.Failed -> "Failed: ${status.error}"
        }
        val color = when (status) {
            is GenerationStatus.Completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            is GenerationStatus.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TTSPlaybackButton(
    isPlaying: Boolean,
    isPaused: Boolean,
    currentRate: Float,
    currentPitch: Float,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onResume: () -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    var showControlMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val playbackAction = {
        when {
            isPlaying -> onPause()
            isPaused -> onResume()
            else -> onPlay()
        }
    }
    val playbackLabel = when {
        isPlaying -> "Pause"
        isPaused -> "Resume"
        else -> "Play"
    }

    Box {
        Box(
            modifier = Modifier
                .size(24.dp)
                .combinedClickable(
                    onClick = playbackAction,
                    onLongClick = { showControlMenu = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Text(
                    text = "||",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = playbackLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showControlMenu,
            onDismissRequest = { showControlMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(playbackLabel) },
                onClick = {
                    showControlMenu = false
                    playbackAction()
                },
                leadingIcon = {
                    if (isPlaying) {
                        Text("||")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
            )
            if (isPlaying || isPaused) {
                DropdownMenuItem(
                    text = { Text("Stop") },
                    onClick = {
                        showControlMenu = false
                        onStop()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Settings")
                        Text(
                            text = "Speed ${String.format("%.1f", currentRate)}x • Pitch ${String.format("%.1f", currentPitch)}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = {
                    showControlMenu = false
                    showSettingsDialog = true
                },
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            )
        }
    }

    if (showSettingsDialog) {
        TTSSettingsDialog(
            currentRate = currentRate,
            currentPitch = currentPitch,
            onRateChange = onRateChange,
            onPitchChange = onPitchChange,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TTSSettingsDialog(
    currentRate: Float,
    currentPitch: Float,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Settings") },
        text = {
            Column {
                Text("Speed: ${String.format("%.1f", currentRate)}x")
                Slider(
                    value = currentRate,
                    onValueChange = onRateChange,
                    valueRange = 0.5f..2.0f
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pitch: ${String.format("%.1f", currentPitch)}x")
                Slider(
                    value = currentPitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.5f..2.0f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
