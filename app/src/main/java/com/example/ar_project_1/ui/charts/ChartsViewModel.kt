package com.example.ar_project_1.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

data class ChartPoint(
    val time: Instant,
    val value: Double,
    val secondary: Double?,
)

data class ChartUiState(
    val type: String = "BLOOD_PRESSURE",
    val points: List<ChartPoint> = emptyList(),
    val unit: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Wykresy pomiarów. Pobieramy do 100 najnowszych punktów danego typu
 * (limit strony backendu), a oś czasu odwracamy do kolejności rosnącej.
 */
class ChartsViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ChartUiState())
        private set

    private var loadJob: Job? = null

    fun setType(type: String) {
        if (state.value.type == type) return
        state.value = state.value.copy(type = type, points = emptyList(), unit = "", error = null)
        load()
    }

    fun load() {
        loadJob?.cancel()
        val requestedType = state.value.type
        state.value = state.value.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val items = repository.measurementSeries(requestedType)
                if (state.value.type != requestedType) return@launch
                val points = items.mapNotNull { it.toPoint() }
                state.value = state.value.copy(
                    points = points,
                    unit = items.firstNotNullOfOrNull { it.text("unit") }.orEmpty(),
                    loading = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać pomiarów.",
                )
            }
        }
    }

    private fun JsonObject.toPoint(): ChartPoint? {
        val time = text("measuredAt")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val value = number("value") ?: return null
        return ChartPoint(time = time, value = value, secondary = number("secondaryValue"))
    }

    private fun JsonObject.text(key: String): String? = get(key)
        ?.takeUnless { it.isJsonNull }
        ?.asString
        ?.takeIf { it.isNotBlank() }

    private fun JsonObject.number(key: String): Double? = text(key)?.toDoubleOrNull()

    companion object {
        val TYPES: List<Pair<String, String>> = listOf(
            "BLOOD_PRESSURE" to "Ciśnienie",
            "HEART_RATE" to "Tętno",
            "WEIGHT" to "Waga",
            "TEMPERATURE" to "Temperatura",
            "GLUCOSE" to "Glukoza",
            "OXYGEN_SATURATION" to "Saturacja",
        )
    }
}
