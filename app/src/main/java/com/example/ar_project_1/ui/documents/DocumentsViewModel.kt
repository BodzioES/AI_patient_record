package com.example.ar_project_1.ui.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.upload.FileSaver
import com.example.ar_project_1.data.upload.PickedFile
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val documents: List<DocumentDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val uploadCompleted: Boolean = false,
    val busyId: String? = null,
    val message: String? = null,
)

class DocumentsViewModel(
    private val repository: PatientCardRepository,
    private val fileSaver: FileSaver,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(DocumentsUiState())
        private set

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val page = repository.documents(page = 0, size = 100)
                state.value = state.value.copy(
                    documents = page.content,
                    loading = false,
                    error = null,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać dokumentów.",
                )
            }
        }
    }

    fun upload(
        picked: PickedFile,
        documentType: String,
        title: String,
        description: String,
        documentDate: String,
    ) {
        val current = state.value
        if (current.uploading) return
        if (title.isBlank()) {
            state.value = current.copy(uploadError = "Tytuł dokumentu jest wymagany.")
            return
        }
        if (documentType.isBlank()) {
            state.value = current.copy(uploadError = "Typ dokumentu jest wymagany.")
            return
        }
        if (picked.sizeBytes <= 0L) {
            state.value = current.copy(uploadError = "Nie można odczytać wybranego pliku.")
            return
        }
        if (picked.sizeBytes > PatientCardRepository.MAX_FILE_BYTES) {
            state.value = current.copy(
                uploadError = "Plik jest za duży. Maksymalny rozmiar to 25 MB.",
            )
            return
        }
        val date = documentDate.trim()
        if (date.isNotEmpty() && !DATE_PATTERN.matches(date)) {
            state.value = current.copy(uploadError = "Data dokumentu musi mieć format RRRR-MM-DD.")
            return
        }

        state.value = current.copy(uploading = true, uploadError = null)
        viewModelScope.launch {
            try {
                repository.uploadDocument(
                    picked = picked,
                    documentType = documentType,
                    title = title.trim(),
                    description = description.trim().takeIf { it.isNotEmpty() },
                    documentDate = date.takeIf { it.isNotEmpty() },
                )
                val page = repository.documents(page = 0, size = 100)
                state.value = state.value.copy(
                    documents = page.content,
                    uploading = false,
                    uploadCompleted = true,
                    message = "Dokument został dodany.",
                )
            } catch (e: AppException) {
                state.value = state.value.copy(uploading = false, uploadError = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    uploading = false,
                    uploadError = e.message ?: "Nie udało się wysłać dokumentu.",
                )
            }
        }
    }

    fun consumeUploadCompleted() {
        state.value = state.value.copy(uploadCompleted = false)
    }

    fun download(document: DocumentDto) {
        if (state.value.busyId != null) return
        state.value = state.value.copy(busyId = document.id, error = null, message = null)
        viewModelScope.launch {
            try {
                val bytes = repository.downloadDocument(document.id)
                val uri: Uri = fileSaver.save(
                    bytes = bytes,
                    fileName = document.originalFilename,
                    mimeType = document.mimeType,
                )
                fileSaver.open(uri, document.mimeType)
                state.value = state.value.copy(
                    busyId = null,
                    message = "Plik zapisany w pamięci podręcznej i otwarty.",
                )
            } catch (e: AppException) {
                state.value = state.value.copy(busyId = null, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    busyId = null,
                    error = e.message ?: "Nie udało się pobrać pliku. Czy jest aplikacja do jego otwarcia?",
                )
            }
        }
    }

    fun delete(document: DocumentDto) {
        if (state.value.busyId != null) return
        state.value = state.value.copy(busyId = document.id, error = null, message = null)
        viewModelScope.launch {
            try {
                repository.deleteDocument(document.id)
                state.value = state.value.copy(
                    busyId = null,
                    documents = state.value.documents.filterNot { it.id == document.id },
                    message = "Dokument został usunięty.",
                )
            } catch (e: AppException) {
                state.value = state.value.copy(busyId = null, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    busyId = null,
                    error = e.message ?: "Nie udało się usunąć dokumentu.",
                )
            }
        }
    }

    companion object {
        val DOCUMENT_TYPES = listOf(
            "LAB_RESULT",
            "DISCHARGE_SUMMARY",
            "PRESCRIPTION",
            "REFERRAL",
            "IMAGING",
            "OTHER",
        )

        private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
