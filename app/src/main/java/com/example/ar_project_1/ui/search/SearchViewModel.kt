package com.example.ar_project_1.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.SearchHitDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val hits: List<SearchHitDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val total: Long = 0,
)

class SearchViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    fun setQuery(value: String) {
        state.value = state.value.copy(query = value, error = null)
        if (value.isBlank()) {
            searchJob?.cancel()
            state.value = state.value.copy(
                hits = emptyList(),
                loading = false,
                searched = false,
                total = 0,
            )
        }
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            searchJob?.cancel()
            state.value = state.value.copy(
                hits = emptyList(),
                loading = false,
                searched = false,
                total = 0,
            )
            return
        }
        searchJob?.cancel()
        state.value = state.value.copy(loading = true, error = null)
        searchJob = viewModelScope.launch {
            try {
                val response = repository.search(query, page = 0, size = PAGE_SIZE)
                if (state.value.query.trim() != query) return@launch
                state.value = state.value.copy(
                    hits = response.content,
                    loading = false,
                    searched = true,
                    total = response.totalElements,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się wyszukać.",
                )
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        state.value = SearchUiState()
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val PAGE_SIZE = 30
    }
}
