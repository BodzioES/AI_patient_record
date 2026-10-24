package com.example.ar_project_1.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.AiSourceDto
import com.example.ar_project_1.data.model.ConversationDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val modelName: String? = null,
    val sources: List<AiSourceDto> = emptyList(),
    val mock: Boolean = false,
)

data class AssistantUiState(
    val conversations: List<ConversationDto> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<ChatUiMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val loadingHistory: Boolean = false,
    val error: String? = null,
    val showHistory: Boolean = false,
)

class AssistantViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(AssistantUiState())
        private set

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val page = repository.conversations()
                state.value = state.value.copy(conversations = page.content)
            } catch (_: Exception) {
                // Conversation history is optional; the chat still works without it.
            }
        }
    }

    fun updateInput(value: String) {
        state.value = state.value.copy(input = value, error = null)
    }

    fun setHistoryVisible(visible: Boolean) {
        state.value = state.value.copy(showHistory = visible)
    }

    fun newConversation() {
        state.value = state.value.copy(
            activeConversationId = null,
            messages = emptyList(),
            input = "",
            error = null,
            showHistory = false,
        )
    }

    fun openConversation(id: String) {
        if (state.value.loadingHistory) return
        state.value = state.value.copy(loadingHistory = true, error = null, showHistory = false)
        viewModelScope.launch {
            try {
                val detail = repository.conversation(id)
                state.value = state.value.copy(
                    activeConversationId = detail.id,
                    messages = detail.messages.map { message ->
                        ChatUiMessage(
                            id = message.id,
                            role = message.role.lowercase(),
                            content = message.content,
                            modelName = message.modelName,
                            sources = message.sources,
                            mock = message.modelName?.contains("mock", ignoreCase = true) == true,
                        )
                    },
                    loadingHistory = false,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(loadingHistory = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loadingHistory = false,
                    error = e.message ?: "Nie udało się wczytać rozmowy.",
                )
            }
        }
    }

    fun send() {
        val current = state.value
        if (current.sending) return
        val message = current.input.trim()
        if (message.isEmpty()) return

        val optimistic = ChatUiMessage(
            id = "local-${System.nanoTime()}",
            role = "user",
            content = message,
        )
        state.value = current.copy(
            messages = current.messages + optimistic,
            input = "",
            sending = true,
            error = null,
        )

        viewModelScope.launch {
            try {
                val response = repository.chat(message, current.activeConversationId)
                state.value = state.value.copy(
                    activeConversationId = response.conversationId,
                    messages = state.value.messages + ChatUiMessage(
                        id = response.assistantMessageId,
                        role = "assistant",
                        content = response.answer,
                        modelName = response.modelName,
                        sources = response.sources,
                        mock = response.mock,
                    ),
                    sending = false,
                )
                loadConversations()
            } catch (e: AppException) {
                state.value = state.value.copy(sending = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    sending = false,
                    error = e.message ?: "Nie udało się wysłać wiadomości.",
                )
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteConversation(id)
                val active = state.value.activeConversationId == id
                state.value = state.value.copy(
                    conversations = state.value.conversations.filterNot { it.id == id },
                    activeConversationId = if (active) null else state.value.activeConversationId,
                    messages = if (active) emptyList() else state.value.messages,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    error = e.message ?: "Nie udało się usunąć rozmowy.",
                )
            }
        }
    }
}
