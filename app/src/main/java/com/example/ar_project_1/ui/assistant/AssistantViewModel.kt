package com.example.ar_project_1.ui.assistant

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.model.AiSourceDto
import com.example.ar_project_1.data.model.ConversationDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.upload.PickedFile
import kotlinx.coroutines.launch

data class ChatUiMessage(
    val id: String,
    val role: String,
    val content: String,
    val modelName: String? = null,
    val sources: List<AiSourceDto> = emptyList(),
    val mock: Boolean = false,
    val imageIds: List<String> = emptyList(),
)

/** Photo picked for the next message; [uploadedId] is set once it is on the server. */
data class ChatAttachment(
    val file: PickedFile,
    val uploadedId: String? = null,
)

data class AssistantUiState(
    val conversations: List<ConversationDto> = emptyList(),
    val activeConversationId: String? = null,
    val messages: List<ChatUiMessage> = emptyList(),
    val input: String = "",
    val attachments: List<ChatAttachment> = emptyList(),
    val sending: Boolean = false,
    val uploadingImages: Boolean = false,
    val loadingHistory: Boolean = false,
    val error: String? = null,
    val showHistory: Boolean = false,
) {
    val canSend: Boolean get() = !sending && (input.isNotBlank() || attachments.isNotEmpty())
    val canAttach: Boolean get() = !sending && attachments.size < MAX_ATTACHMENTS

    companion object {
        const val MAX_ATTACHMENTS = 4
    }
}

class AssistantViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = mutableStateOf(AssistantUiState())
        private set

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            // Historia jest opcjonalna — czat działa także bez niej.
            runCatching { repository.conversations() }
                .onSuccess { state.value = state.value.copy(conversations = it.content) }
        }
    }

    fun updateInput(value: String) {
        state.value = state.value.copy(input = value, error = null)
    }

    fun addAttachment(file: PickedFile) {
        val current = state.value
        if (!current.canAttach) return
        val error = when {
            file.sizeBytes <= 0L -> "Nie można odczytać wybranego zdjęcia."
            file.sizeBytes > PatientCardRepository.MAX_FILE_BYTES -> "Zdjęcie jest za duże (maks. 25 MB)."
            else -> null
        }
        state.value = if (error != null) {
            current.copy(error = error)
        } else {
            current.copy(attachments = current.attachments + ChatAttachment(file), error = null)
        }
    }

    fun removeAttachment(attachment: ChatAttachment) {
        state.value = state.value.copy(attachments = state.value.attachments - attachment)
    }

    fun setHistoryVisible(visible: Boolean) {
        state.value = state.value.copy(showHistory = visible)
    }

    fun newConversation() {
        if (state.value.sending) return
        state.value = state.value.copy(
            activeConversationId = null,
            messages = emptyList(),
            input = "",
            attachments = emptyList(),
            error = null,
            showHistory = false,
        )
    }

    fun openConversation(id: String) {
        if (state.value.loadingHistory) return
        state.value = state.value.copy(loadingHistory = true, error = null, showHistory = false)
        viewModelScope.launch {
            state.value = try {
                val detail = repository.conversation(id)
                state.value.copy(
                    activeConversationId = detail.id,
                    messages = detail.messages.map { message ->
                        ChatUiMessage(
                            id = message.id,
                            role = message.role.lowercase(),
                            content = message.content,
                            modelName = message.modelName,
                            sources = message.sources,
                            mock = message.modelName?.contains("mock", ignoreCase = true) == true,
                            imageIds = message.imageIds.orEmpty(),
                        )
                    },
                    loadingHistory = false,
                )
            } catch (e: Exception) {
                state.value.copy(loadingHistory = false, error = e.message ?: "Nie udało się wczytać rozmowy.")
            }
        }
    }

    fun send() {
        val current = state.value
        if (!current.canSend) return
        val message = current.input.trim()
        val localId = "local-${System.nanoTime()}"
        state.value = current.copy(input = "", sending = true, error = null)

        viewModelScope.launch {
            try {
                val imageIds = uploadAttachments()
                state.value = state.value.copy(
                    attachments = emptyList(),
                    messages = state.value.messages + ChatUiMessage(
                        id = localId,
                        role = "user",
                        content = message,
                        imageIds = imageIds,
                    ),
                )
                val response = repository.chat(message, current.activeConversationId, imageIds)
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
            } catch (e: Exception) {
                // Przywracamy treść, aby można było ponowić bez przepisywania;
                // już wysłane zdjęcia zachowują swoje id i nie zostaną wysłane drugi raz.
                state.value = state.value.copy(
                    messages = state.value.messages.filterNot { it.id == localId },
                    sending = false,
                    uploadingImages = false,
                    input = message,
                    error = e.message ?: "Nie udało się wysłać wiadomości.",
                )
            }
        }
    }

    private suspend fun uploadAttachments(): List<String> {
        val pending = state.value.attachments
        if (pending.none { it.uploadedId == null }) return pending.mapNotNull { it.uploadedId }
        state.value = state.value.copy(uploadingImages = true)
        val uploaded = pending.map { attachment ->
            if (attachment.uploadedId != null) return@map attachment
            val image = repository.uploadImage(
                picked = attachment.file,
                title = "Załącznik czatu AI",
                description = null,
                category = "OTHER",
                symptomId = null,
                capturedAt = null,
            )
            attachment.copy(uploadedId = image.id).also { done ->
                state.value = state.value.copy(
                    attachments = state.value.attachments.map { if (it == attachment) done else it },
                )
            }
        }
        state.value = state.value.copy(uploadingImages = false)
        return uploaded.mapNotNull { it.uploadedId }
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
            } catch (e: Exception) {
                state.value = state.value.copy(error = e.message ?: "Nie udało się usunąć rozmowy.")
            }
        }
    }
}
