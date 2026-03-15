package com.naigebao.sessions.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.naigebao.sessions.viewmodel.SessionItemUi
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionItemCard(
    session: SessionItemUi,
    onClick: (String) -> Unit,
    onPinClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(session.id) },
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SessionAvatar(title = session.title)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (session.isPinned) {
                            Text(
                                text = "PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        HighlightedText(
                            text = session.highlightedTitle ?: session.title,
                            regularText = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HighlightedText(
                        text = session.highlightedPreview ?: session.preview,
                        regularText = session.preview,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(session.updatedAt),
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (session.unreadCount > 0) {
                    UnreadBadge(count = session.unreadCount)
                }
                if (onPinClick != null) {
                    IconButton(
                        onClick = { onPinClick(session.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text(
                            text = if (session.isPinned) "PIN" else "PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (session.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    regularText: String,
    style: androidx.compose.ui.text.TextStyle,
    maxLines: Int
) {
    if (text.contains("⟨") && text.contains("⟩")) {
        val annotatedString = buildAnnotatedString {
            var currentIndex = 0
            val pattern = Regex("⟨([^⟩]+)⟩")
            pattern.findAll(text).forEach { match ->
                append(regularText.substring(currentIndex, match.range.first))
                withStyle(SpanStyle(background = MaterialTheme.colorScheme.tertiaryContainer)) {
                    append(match.groupValues[1])
                }
                currentIndex = match.range.last + 1
            }
            if (currentIndex < regularText.length) {
                append(regularText.substring(currentIndex))
            }
        }
        Text(
            text = annotatedString,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = regularText,
            style = style,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionAvatar(title: String) {
    val initials = title.trim().take(2).uppercase(Locale.getDefault())
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = android.R.drawable.sym_def_app_icon,
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}