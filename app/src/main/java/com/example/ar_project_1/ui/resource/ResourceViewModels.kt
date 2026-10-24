package com.example.ar_project_1.ui.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.resource.RecordRequestBuilder
import com.example.ar_project_1.data.resource.RecordSummary
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.resource.ResourceType
import kotlinx.coroutines.launch

data class ResourceRow(
    val id: String,
    val summary: RecordSummary,
)

data class ResourceListUiState(
    val rows: List<ResourceRow> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val deletingId: String? = null,
)

class ResourceListViewModel(
    private val repository: PatientCardRepository,
    private val type: ResourceType,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ResourceListUiState())
        private set

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val page = repository.list(type, page = 0, size = 100)
                state.value = ResourceListUiState(
                    rows = page.content.mapNotNull { json ->
                        val id = json.get("id")?.takeUnless { it.isJsonNull }?.asString
                            ?: return@mapNotNull null
                        ResourceRow(id, ResourceCatalog.summary(type, json))
                    },
                    loading = false,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać listy.",
                )
            }
        }
    }

    fun delete(id: String) {
        if (state.value.deletingId != null) return
        state.value = state.value.copy(deletingId = id, error = null)
        viewModelScope.launch {
            try {
                repository.delete(type, id)
                state.value = state.value.copy(
                    deletingId = null,
                    rows = state.value.rows.filterNot { it.id == id },
                )
            } catch (e: AppException) {
                state.value = state.value.copy(deletingId = null, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    deletingId = null,
                    error = e.message ?: "Nie udało się usunąć wpisu.",
                )
            }
        }
    }
}

data class ResourceFormUiState(
    val values: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class ResourceFormViewModel(
    private val repository: PatientCardRepository,
    private val type: ResourceType,
    private val id: String?,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(
        ResourceFormUiState(
            values = RecordRequestBuilder.initialValues(type),
            loading = !id.isNullOrBlank(),
        ),
    )
        private set

    init {
        if (!id.isNullOrBlank()) load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                val json = repository.get(type, id!!)
                state.value = ResourceFormUiState(
                    values = RecordRequestBuilder.prefill(type, json),
                    loading = false,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać wpisu.",
                )
            }
        }
    }

    fun update(key: String, value: String) {
        state.value = state.value.copy(
            values = state.value.values + (key to value),
            error = null,
        )
    }

    fun save() {
        val current = state.value
        if (current.saving) return
        try {
            RecordRequestBuilder.validateRequired(type, current.values)
        } catch (e: IllegalArgumentException) {
            state.value = current.copy(error = e.message)
            return
        }
        state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                if (id.isNullOrBlank()) {
                    repository.create(type, current.values)
                } else {
                    repository.update(type, id, current.values)
                }
                state.value = state.value.copy(saving = false, saved = true)
            } catch (e: AppException) {
                state.value = state.value.copy(saving = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    saving = false,
                    error = e.message ?: "Nie udało się zapisać wpisu.",
                )
            }
        }
    }

    fun consumeSaved() {
        state.value = state.value.copy(saved = false)
    }
}
