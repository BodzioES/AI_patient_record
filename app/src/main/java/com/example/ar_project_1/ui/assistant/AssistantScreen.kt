package com.example.ar_project_1.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.AiStatusDto
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.aiIsMock
import com.example.ar_project_1.ui.common.aiModeLabel

@Composable
fun AssistantScreen(
    container: AppContainer,
    onOpenImage: ((String) -> Unit)? = null,
) {
    val viewModel: AssistantViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AssistantViewModel(container.repository) }
        },
    )
    val state = viewModel.state.value
    val aiStatus = LocalAiStatus.current
    val mock = aiIsMock(aiStatus)
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.sending) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        StatusBanner(aiStatus, mock)
        Toolbar(state, viewModel)
        state.error?.let { ErrorBanner(message = it, modifier = Modifier.padding(horizontal = 16.dp)) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    EmptyState(
                        title = if (mock) "Asystent AI (MOCK)" else "Asystent AI",
                        message = "Zadaj pytanie o swoje zapisane dane albo dołącz zdjęcie przyciskiem obok pola tekstu." +
                            if (mock) " Odpowiedzi są demonstracyjne." else " Odpowiada model ${aiModeLabel(aiStatus)}.",
                    )
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message, container.imageLoader, onOpenImage)
                }
            }
            if (state.sending) {
                item { Progress(if (state.uploadingImages) "Wysyłanie zdjęć…" else "Asystent pisze…") }
            }
        }

        ChatInputBar(state, viewModel)
    }

    if (state.showHistory) {
        HistoryDialog(
            conversations = state.conversations,
            onOpen = viewModel::openConversation,
            onDelete = viewModel::deleteConversation,
            onDismiss = { viewModel.setHistoryVisible(false) },
        )
    }
}

@Composable
private fun StatusBanner(aiStatus: AiStatusDto?, mock: Boolean) {
    val text = when {
        aiStatus == null -> "Asystent AI: sprawdzanie trybu pracy… To nie jest porada medyczna ani diagnoza."
        mock -> "Tryb MOCK: odpowiedzi demonstracyjne. To nie jest porada medyczna ani diagnoza."
        else -> "Odpowiada lokalny model ${aiModeLabel(aiStatus)}. To nie jest porada medyczna ani diagnoza. " +
            "Dane pozostają na Twoim serwerze."
    }
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun Toolbar(state: AssistantUiState, viewModel: AssistantViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = viewModel::newConversation, enabled = !state.sending) {
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
}

@Composable
private fun Progress(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
