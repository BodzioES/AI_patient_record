package com.example.ar_project_1.ui.assistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.aiIsMock
import com.example.ar_project_1.ui.common.aiModeLabel

@Composable
fun AssistantScreen(container: AppContainer) {
    val viewModel: AssistantViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AssistantViewModel(container.repository) }
        },
    )
    val state = viewModel.state.value
    val aiStatus = LocalAiStatus.current
    val mock = aiIsMock(aiStatus)
    val bannerText = when {
        aiStatus == null -> "Asystent AI: sprawdzanie trybu pracy… To nie jest porada medyczna ani diagnoza."
        mock -> "Tryb MOCK: odpowiedzi demonstracyjne. To nie jest porada medyczna ani diagnoza."
        else -> "Odpowiada lokalny model ${aiModeLabel(aiStatus)}. To nie jest porada medyczna ani diagnoza. " +
            "Dane pozostają na Twoim serwerze."
    }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    bannerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = viewModel::newConversation) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Nowa")
            }
            TextButton(onClick = { viewModel.setHistoryVisible(true) }) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Historia (${state.conversations.size})")
            }
            if (state.loadingHistory) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        if (state.error != null) {
            ErrorBanner(
                message = state.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState(
                        title = if (mock) "Asystent AI (MOCK)" else "Asystent AI",
                        message = if (mock) {
                            "Zadaj pytanie o swoje zapisane dane. Odpowiedzi są demonstracyjne."
                        } else {
                            "Zadaj pytanie o swoje zapisane dane. Odpowiada model ${aiModeLabel(aiStatus)}."
                        },
                    )
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
            if (state.sending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Asystent pisze…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Napisz pytanie…") },
                    maxLines = 4,
                    enabled = !state.sending,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::send,
                    enabled = state.input.isNotBlank() && !state.sending,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Wyślij")
                }
            }
        }
    }

    if (state.showHistory) {
        AlertDialog(
            onDismissRequest = { viewModel.setHistoryVisible(false) },
            title = { Text("Historia rozmów") },
            text = {
                if (state.conversations.isEmpty()) {
                    Text("Brak zapisanych rozmów.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(state.conversations, key = { it.id }) { conversation ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openConversation(conversation.id) }
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
                                        "${conversation.messageCount} wiad. • " +
                                            DateInputs.toDisplay(conversation.updatedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteConversation(conversation.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Usuń rozmowę")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setHistoryVisible(false) }) { Text("Zamknij") }
            },
        )
    }
}

@Composable
private fun MessageBubble(message: ChatUiMessage) {
    val fromUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = if (fromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (fromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (!fromUser && (message.mock || !message.modelName.isNullOrBlank())) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        buildString {
                            if (message.mock) append("MOCK")
                            if (message.mock && !message.modelName.isNullOrBlank()) append(" • ")
                            if (!message.modelName.isNullOrBlank()) append(message.modelName)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (message.sources.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Źródła: " + message.sources.joinToString(", ") { source ->
                            source.type.lowercase().replace('_', ' ')
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}
