package com.example.ar_project_1.ui.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.resource.RecordRequestBuilder
import com.example.ar_project_1.data.resource.RecordSummary
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.resource.ResourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class ResourceRow(
    val id: String,
    val summary: RecordSummary,
)

data class ResourceSort(val label: String, val value: String)

data class ResourceListUiState(
    val rows: List<ResourceRow> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val deletingId: String? = null,
    val page: Int = 0,
    val last: Boolean = true,
    val total: Long = 0,
    val sort: String = "",
    /** Filtr tekstowy działa na już pobranych wpisach. */
    val filter: String = "",
)

class ResourceListViewModel(
    private val repository: PatientCardRepository,
    private val type: ResourceType,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(
        ResourceListUiState(sort = sortsFor(type).first().value),
    )
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
                val page = repository.list(
                    type = type,
                    page = 0,
                    size = PAGE_SIZE,
                    sort = requestedSort.ifBlank { null },
                )
                if (state.value.sort != requestedSort) return@launch
                state.value = state.value.copy(
                    rows = page.content.mapNotNull { it.toRow() },
                    loading = false,
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
                    error = e.message ?: "Nie udało się pobrać listy.",
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
                val page = repository.list(
                    type = type,
                    page = nextPage,
                    size = PAGE_SIZE,
                    sort = current.sort.ifBlank { null },
                )
                if (state.value.sort != current.sort) return@launch
                state.value = state.value.copy(
                    rows = state.value.rows + page.content.mapNotNull { it.toRow() },
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
                    error = e.message ?: "Nie udało się pobrać kolejnych wpisów.",
                )
            }
        }
    }

    fun setSort(value: String) {
        if (state.value.sort == value) return
        state.value = state.value.copy(sort = value)
        load()
    }

    fun setFilter(value: String) {
        state.value = state.value.copy(filter = value)
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
                    total = (state.value.total - 1).coerceAtLeast(0),
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

    private fun com.google.gson.JsonObject.toRow(): ResourceRow? {
        val id = get("id")?.takeUnless { it.isJsonNull }?.asString ?: return null
        return ResourceRow(id, ResourceCatalog.summary(type, this))
    }

    companion object {
        const val PAGE_SIZE = 50

        fun sortsFor(type: ResourceType): List<ResourceSort> = when (type) {
            ResourceType.ALLERGIES -> listOf(
                ResourceSort("Substancja A–Z", "substance,asc"),
                ResourceSort("Najnowsze", "createdAt,desc"),
                ResourceSort("Najstarsze", "createdAt,asc"),
            )
            ResourceType.CONDITIONS -> listOf(
                ResourceSort("Nazwa A–Z", "name,asc"),
                ResourceSort("Najnowsze", "createdAt,desc"),
                ResourceSort("Najstarsze", "createdAt,asc"),
            )
            ResourceType.MEDICATIONS -> listOf(
                ResourceSort("Nazwa A–Z", "name,asc"),
                ResourceSort("Najnowsze", "createdAt,desc"),
                ResourceSort("Najstarsze", "createdAt,asc"),
            )
            ResourceType.SYMPTOMS -> listOf(
                ResourceSort("Najnowsze", "observedAt,desc"),
                ResourceSort("Najstarsze", "observedAt,asc"),
                ResourceSort("Tytuł A–Z", "title,asc"),
            )
            ResourceType.MEASUREMENTS -> listOf(
                ResourceSort("Najnowsze", "measuredAt,desc"),
                ResourceSort("Najstarsze", "measuredAt,asc"),
            )
            ResourceType.DOCTORS -> listOf(
                ResourceSort("Nazwisko A–Z", "lastName,asc"),
                ResourceSort("Najnowsze", "createdAt,desc"),
            )
            ResourceType.APPOINTMENTS -> listOf(
                ResourceSort("Najnowsze", "scheduledAt,desc"),
                ResourceSort("Najstarsze", "scheduledAt,asc"),
            )
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
