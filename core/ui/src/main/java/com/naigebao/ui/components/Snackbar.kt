package com.naigebao.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

enum class SnackbarType {
 SUCCESS, ERROR, INFO
}

data class SnackbarMessage(
 val id: Long = System.currentTimeMillis(),
 val message: String,
 val type: SnackbarType = SnackbarType.INFO,
 val duration: Long = 3000L,
 val actionLabel: String? = null,
 val onAction: (() -> Unit)? = null,
 val onDismiss: (() -> Unit)? = null
)

@Composable
fun SnackbarHost(
 snackbarMessages: List<SnackbarMessage>,
 onDismiss: (SnackbarMessage) -> Unit,
 modifier: Modifier = Modifier
) {
 Column(
  modifier = modifier
   .fillMaxWidth()
   .padding(16.dp),
  verticalArrangement = Arrangement.spacedBy(8.dp)
 ) {
  snackbarMessages.forEach { message ->
   SnackbarItem(
    message = message,
    onDismiss = { onDismiss(message) }
   )
  }
 }
}

@Composable
private fun SnackbarItem(
 message: SnackbarMessage,
 onDismiss: () -> Unit
) {
 var visible by remember { mutableStateOf(true) }

 LaunchedEffect(message.id) {
  if (message.duration > 0) {
   delay(message.duration)
   visible = false
   delay(300)
   onDismiss()
  }
 }

 AnimatedVisibility(
  visible = visible,
  enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
  exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
 ) {
  Snackbar(
   message = message.message,
   type = message.type,
   actionLabel = message.actionLabel,
   onAction = message.onAction,
   onDismiss = {
    visible = false
    onDismiss()
   }
  )
 }
}

@Composable
private fun Snackbar(
 message: String,
 type: SnackbarType,
 actionLabel: String? = null,
 onAction: (() -> Unit)? = null,
 onDismiss: () -> Unit
) {
 val (backgroundColor, icon, iconTint) = when (type) {
  SnackbarType.SUCCESS -> Triple(
   MaterialTheme.colorScheme.primaryContainer,
   Icons.Default.CheckCircle,
   MaterialTheme.colorScheme.primary
  )
  SnackbarType.ERROR -> Triple(
   MaterialTheme.colorScheme.errorContainer,
   Icons.Default.Info,
   MaterialTheme.colorScheme.error
  )
  SnackbarType.INFO -> Triple(
   MaterialTheme.colorScheme.secondaryContainer,
   Icons.Default.Info,
   MaterialTheme.colorScheme.secondary
  )
 }

 Surface(
  modifier = Modifier.fillMaxWidth(),
  shape = RoundedCornerShape(8.dp),
  color = backgroundColor,
  tonalElevation = 4.dp,
  shadowElevation = 4.dp
 ) {
  Row(
   modifier = Modifier
    .fillMaxWidth()
    .padding(12.dp),
   verticalAlignment = Alignment.CenterVertically
  ) {
   Icon(
    imageVector = icon,
    contentDescription = null,
    tint = iconTint,
    modifier = Modifier.size(20.dp)
   )

   Spacer(modifier = Modifier.width(12.dp))

   Text(
    text = message,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.weight(1f)
   )

   if (actionLabel != null && onAction != null) {
    Text(
     text = actionLabel,
     style = MaterialTheme.typography.labelMedium,
     color = MaterialTheme.colorScheme.primary,
     modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .clickable { onAction() }
      .padding(4.dp)
    )
   }

   IconButton(
    onClick = onDismiss,
    modifier = Modifier.size(24.dp)
   ) {
    Icon(
     imageVector = Icons.Default.Close,
     contentDescription = "Dismiss",
     tint = MaterialTheme.colorScheme.onSurfaceVariant,
     modifier = Modifier.size(16.dp)
    )
   }
  }
 }
}

class SnackbarManager {
 private val _messages = mutableListOf<SnackbarMessage>()
 val messages: List<SnackbarMessage> get() = _messages.toList()

 fun showMessage(message: SnackbarMessage) {
  _messages.add(message)
 }

 fun dismiss(message: SnackbarMessage) {
  _messages.remove(message)
 }

 fun showSuccess(message: String, duration: Long = 3000L) {
  showMessage(SnackbarMessage(message = message, type = SnackbarType.SUCCESS, duration = duration))
 }

 fun showError(message: String, duration: Long = 5000L) {
  showMessage(SnackbarMessage(message = message, type = SnackbarType.ERROR, duration = duration))
 }

 fun showInfo(message: String, duration: Long = 3000L) {
  showMessage(SnackbarMessage(message = message, type = SnackbarType.INFO, duration = duration))
 }
}