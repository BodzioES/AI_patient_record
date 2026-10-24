package com.example.ar_project_1.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.model.TimelineEventDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.resource.ResourceType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class HomeUiState(
    val patient: PatientDto? = null,
    val counts: Map<ResourceType, Long> = emptyMap(),
    val timeline: List<TimelineEventDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(HomeUiState())
        private set

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val patient = repository.patient()
                val counts = supervisorScope {
                    ResourceType.entries.map { type ->
                        async {
                            type to runCatching { repository.list(type, page = 0, size = 1).totalElements }
                                .getOrDefault(0L)
                        }
                    }.awaitAll().toMap()
                }
                val timeline = runCatching {
                    repository.timeline(page = 0, size = PREVIEW_EVENTS).content
                }.getOrDefault(emptyList())
                state.value = HomeUiState(
                    patient = patient,
                    counts = counts,
                    timeline = timeline,
                    loading = false,
                )
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać danych.",
                )
            }
        }
    }

    private companion object {
        const val PREVIEW_EVENTS = 5
    }
}
