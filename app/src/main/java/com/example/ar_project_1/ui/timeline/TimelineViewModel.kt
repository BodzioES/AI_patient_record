package com.example.ar_project_1.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.TimelineEventDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class TimelineUiState(
    val events: List<TimelineEventDto> = emptyList(),
    /** null = wszystkie rodzaje zdarzeń. */
    val filter: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 0,
    val last: Boolean = true,
    val total: Long = 0,
)

class TimelineViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(TimelineUiState())
        private set

    private var loadJob: Job? = null
    private var moreJob: Job? = null

    fun setFilter(type: String?) {
        if (state.value.filter == type) return
        state.value = state.value.copy(filter = type)
        load()
    }

    fun load() {
        loadJob?.cancel()
        moreJob?.cancel()
        val requestedFilter = state.value.filter
        state.value = state.value.copy(loading = true, loadingMore = false, error = null)
        loadJob = viewModelScope.launch {
            try {
                val response = repository.timeline(
                    page = 0,
                    size = PAGE_SIZE,
                    types = typesFor(requestedFilter),
                )
                if (state.value.filter != requestedFilter) return@launch
                state.value = state.value.copy(
                    events = response.content,
                    loading = false,
                    page = 0,
                    last = response.last,
                    total = response.totalElements,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać chronologii.",
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
                val response = repository.timeline(
                    page = nextPage,
                    size = PAGE_SIZE,
                    types = typesFor(current.filter),
                )
                if (state.value.filter != current.filter) return@launch
                state.value = state.value.copy(
                    events = state.value.events + response.content,
                    loadingMore = false,
                    page = nextPage,
                    last = response.last,
                    total = response.totalElements,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state.value = state.value.copy(loadingMore = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loadingMore = false,
                    error = e.message ?: "Nie udało się pobrać kolejnych zdarzeń.",
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20

        val FILTERS: List<Pair<String?, String>> = listOf(
            null to "Wszystko",
            "APPOINTMENT" to "Wizyty",
            "MEASUREMENT" to "Pomiary",
            "SYMPTOM" to "Objawy",
            "MEDICATION" to "Leki",
            "DOCUMENT" to "Dokumenty",
            "IMAGE" to "Zdjęcia",
        )

        fun typesFor(filter: String?): List<String> =
            if (filter == null) emptyList() else listOf(filter)
    }
}
