package com.naigebao.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naigebao.chat.viewmodel.AiModel
import com.naigebao.chat.viewmodel.Attachment

@Composable
fun MessageInput(
 value: String,
 onValueChange: (String) -> Unit,
 onSend: () -> Unit,
 selectedModel: AiModel,
 availableModels: List<AiModel>,
 onModelSelected: (AiModel) -> Unit,
 isGenerating: Boolean,
 onStopGeneration: () -> Unit,
 attachments: List<Attachment>,
 onRequestAttachment: () -> Unit,
 onRemoveAttachment: (Attachment) -> Unit,
 modifier: Modifier = Modifier
) {
 var showModelSelector by remember { mutableStateOf(false) }

 Column(modifier = modifier.fillMaxWidth()) {
  if (attachments.isNotEmpty()) {
   Row(
    modifier = Modifier
     .fillMaxWidth()
     .horizontalScroll(rememberScrollState())
     .padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
   ) {
    attachments.forEach { attachment ->
     AttachmentPreview(
      attachment = attachment,
      onRemove = { onRemoveAttachment(attachment) }
     )
    }
   }
  }

  Row(
   modifier = Modifier
    .fillMaxWidth()
    .padding(12.dp),
   verticalAlignment = Alignment.CenterVertically
  ) {
   Box {
    Text(
     text = selectedModel.displayName,
     style = MaterialTheme.typography.labelMedium,
     color = MaterialTheme.colorScheme.primary,
     modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .clickable { showModelSelector = !showModelSelector }
      .padding(horizontal = 8.dp, vertical = 4.dp)
    )
    if (showModelSelector) {
     Column(
      modifier = Modifier
       .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
       .padding(4.dp)
     ) {
      availableModels.forEach { model ->
       TextButton(onClick = {
        onModelSelected(model)
        showModelSelector = false
       }) {
        Text(model.displayName)
       }
      }
     }
    }
   }

   Spacer(modifier = Modifier.width(8.dp))

   TextButton(onClick = onRequestAttachment) {
    Text("+Attach")
   }

   Spacer(modifier = Modifier.width(8.dp))

   OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = { Text("Message") },
    modifier = Modifier.weight(1f),
    maxLines = 4
   )

   Spacer(modifier = Modifier.width(8.dp))

   if (isGenerating) {
    Button(onClick = onStopGeneration) {
     Text("Stop")
    }
   } else {
    Button(
     onClick = onSend,
     enabled = value.isNotBlank() || attachments.isNotEmpty()
    ) {
     Text("Send")
    }
   }
  }
 }
}

@Composable
private fun AttachmentPreview(
 attachment: Attachment,
 onRemove: () -> Unit
) {
 Box(
  modifier = Modifier
   .size(60.dp)
   .clip(RoundedCornerShape(8.dp))
   .background(MaterialTheme.colorScheme.surfaceVariant)
 ) {
  Column(
   modifier = Modifier
    .fillMaxWidth()
    .padding(4.dp),
   horizontalAlignment = Alignment.CenterHorizontally,
   verticalArrangement = Arrangement.Center
  ) {
   Text(
    text = if (attachment.mimeType.startsWith("image/")) "IMG" else "FILE",
    style = MaterialTheme.typography.labelSmall
   )
   Text(
    text = attachment.name,
    style = MaterialTheme.typography.labelSmall,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(horizontal = 2.dp)
   )
  }

  Text(
   text = "X",
   style = MaterialTheme.typography.labelSmall,
   color = MaterialTheme.colorScheme.error,
   modifier = Modifier
    .align(Alignment.TopEnd)
    .padding(2.dp)
    .clickable { onRemove() }
  )
 }
}
