package com.example.ar_project_1.ui.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ar_project_1.data.model.ConversationDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.upload.describe
import com.example.ar_project_1.ui.common.RemoteImage
import com.example.ar_project_1.ui.common.RemoteImageLoader

@Composable
internal fun MessageBubble(
    message: ChatUiMessage,
    loader: RemoteImageLoader,
    onOpenImage: ((String) -> Unit)?,
) {
    val fromUser = message.role == "user"
    val textColor = if (fromUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = if (fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp)) {
                if (message.imageIds.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(message.imageIds) { id ->
                            RemoteImage(
                                imageId = id,
                                loader = loader,
                                contentDescription = "Załączone zdjęcie",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = onOpenImage != null) { onOpenImage?.invoke(id) },
                            )
                        }
                    }
                    if (message.content.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (message.content.isNotBlank()) {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium, color = textColor)
                }
                val meta = listOfNotNull(
                    "MOCK".takeIf { !fromUser && message.mock },
                    message.modelName?.takeIf { !fromUser && it.isNotBlank() },
                ).joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                if (message.sources.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Źródła: " + message.sources.joinToString(", ") { it.type.lowercase().replace('_', ' ') },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChatInputBar(
    state: AssistantUiState,
    viewModel: AssistantViewModel,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.describe(uri) }
                .onSuccess(viewModel::addAttachment)
        }
    }
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            if (state.attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    items(state.attachments) { attachment ->
                        InputChip(
                            selected = attachment.uploadedId != null,
                            onClick = { if (!state.sending) viewModel.removeAttachment(attachment) },
                            label = {
                                Text(
                                    attachment.file.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 2.dp),
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Usuń załącznik") },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = state.canAttach,
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Dołącz zdjęcie")
                }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(if (state.attachments.isEmpty()) "Napisz pytanie…" else "Zapytaj o zdjęcie…")
                    },
                    maxLines = 4,
                    enabled = !state.sending,
                )
                IconButton(onClick = viewModel::send, enabled = state.canSend) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij")
                }
            }
        }
    }
}

@Composable
internal fun HistoryDialog(
    conversations: List<ConversationDto>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historia rozmów") },
        text = {
            if (conversations.isEmpty()) {
                Text("Brak zapisanych rozmów.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(conversations, key = { it.id }) { conversation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(conversation.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    conversation.title.ifBlank { "Rozmowa" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "${conversation.messageCount} wiad. • " + DateInputs.toDisplay(conversation.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onDelete(conversation.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń rozmowę")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } },
    )
}
