package com.example.ar_project_1.ui.images

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

data class ImageAnalysisUi(
    val text: String,
    val modelName: String,
    val modelVersion: String?,
    val mock: Boolean,
    val notice: String,
)

data class ImageDetailUiState(
    val info: ImageDto? = null,
    val analyzing: Boolean = false,
    val analysisError: String? = null,
    val analysis: ImageAnalysisUi? = null,
)

/** Metadata and AI analysis of one image; pixels come from [com.example.ar_project_1.ui.common.RemoteImageLoader]. */
class ImageDetailViewModel(
    private val repository: PatientCardRepository,
    private val imageId: String,
) : ViewModel() {

    var state = mutableStateOf(ImageDetailUiState())
        private set

    init {
        viewModelScope.launch {
            runCatching { repository.image(imageId) }
                .onSuccess { state.value = state.value.copy(info = it) }
        }
    }

    fun analyze() {
        if (state.value.analyzing) return
        state.value = state.value.copy(analyzing = true, analysisError = null)
        viewModelScope.launch {
            state.value = try {
                state.value.copy(analyzing = false, analysis = repository.analyzeImage(imageId).toAnalysis())
            } catch (e: Exception) {
                state.value.copy(analyzing = false, analysisError = e.message ?: "Nie udało się wykonać analizy.")
            }
        }
    }

    private fun JsonObject.toAnalysis(): ImageAnalysisUi {
        fun text(key: String): String? = get(key)
            ?.takeUnless { it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }

        return ImageAnalysisUi(
            text = text("analysisText") ?: "Brak treści analizy.",
            modelName = text("modelName") ?: "mock",
            modelVersion = text("modelVersion"),
            mock = get("mock")?.takeUnless { it.isJsonNull }?.asBoolean ?: true,
            notice = text("notice") ?: "Wynik demonstracyjny. To nie jest diagnoza medyczna.",
        )
    }
}
