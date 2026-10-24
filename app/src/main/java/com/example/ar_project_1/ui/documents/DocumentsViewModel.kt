package com.example.ar_project_1.ui.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.upload.FileSaver
import com.example.ar_project_1.data.upload.PickedFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val documents: List<DocumentDto> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val uploadCompleted: Boolean = false,
    val busyId: String? = null,
    val message: String? = null,
    val page: Int = 0,
    val last: Boolean = true,
    val total: Long = 0,
    val sort: String = "uploadedAt,desc",
)

class DocumentsViewModel(
    private val repository: PatientCardRepository,
    private val fileSaver: FileSaver,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(DocumentsUiState())
        private set

    private var loadJob: Job? = null
    private var moreJob: Job? = null

    fun load() {
        loadJob?.cancel()
        moreJob?.cancel()
        val requestedSort = state.value.sort
        state.value = state.value.copy(loading = true, loadingMore = false, error = null)
        loadJob = viewModelScope.launch {
            try {
                val page = repository.documents(page = 0, size = PAGE_SIZE, sort = requestedSort)
                if (state.value.sort != requestedSort) return@launch
                state.value = state.value.copy(
                    documents = page.content,
                    loading = false,
                    error = null,
                    page = 0,
                    last = page.last,
                    total = page.totalElements,
                )
            } catch (e: CancellationException) {
                throw e
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

    fun loadMore() {
        val current = state.value
        if (current.loading || current.loadingMore || current.last) return
        moreJob?.cancel()
        state.value = current.copy(loadingMore = true, error = null)
        moreJob = viewModelScope.launch {
            try {
                val nextPage = current.page + 1
                val page = repository.documents(page = nextPage, size = PAGE_SIZE, sort = current.sort)
                if (state.value.sort != current.sort) return@launch
                state.value = state.value.copy(
                    documents = state.value.documents + page.content,
                    loadingMore = false,
                    page = nextPage,
                    last = page.last,
                    total = page.totalElements,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state.value = state.value.copy(loadingMore = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loadingMore = false,
                    error = e.message ?: "Nie udało się pobrać kolejnych dokumentów.",
                )
            }
        }
    }

    fun setSort(value: String) {
        if (state.value.sort == value) return
        state.value = state.value.copy(sort = value)
        load()
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
                val page = repository.documents(
                    page = 0,
                    size = PAGE_SIZE,
                    sort = state.value.sort,
                )
                state.value = state.value.copy(
                    documents = page.content,
                    uploading = false,
                    uploadCompleted = true,
                    message = "Dokument został dodany.",
                    page = 0,
                    last = page.last,
                    total = page.totalElements,
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
        const val PAGE_SIZE = 50

        val SORTS: List<Pair<String, String>> = listOf(
            "Najnowsze" to "uploadedAt,desc",
            "Najstarsze" to "uploadedAt,asc",
            "Tytuł A–Z" to "title,asc",
        )

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
