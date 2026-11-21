package com.example.ar_project_1.ui.images

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.upload.PickedFile
import com.example.ar_project_1.ui.common.RemoteImageLoader
import kotlinx.coroutines.launch

data class ImagesUiState(
    val images: List<ImageDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val uploadCompleted: Boolean = false,
    val busyId: String? = null,
    val message: String? = null,
    /** null = wszystkie kategorie. */
    val category: String? = null,
    val groupSeries: Boolean = true,
)

/** Seria zdjęć powiązanych z tym samym objawem (albo pojedyncze zdjęcie). */
data class ImageSeries(
    val key: String,
    val title: String,
    val images: List<ImageDto>,
)

/** Pola formularza dodawania zdjęcia. */
data class ImageUploadForm(
    val title: String = "",
    val description: String = "",
    val category: String = ImagesViewModel.CATEGORIES.first(),
    val symptomId: String = "",
    val capturedAt: String = "",
)

class ImagesViewModel(
    private val repository: PatientCardRepository,
    private val imageLoader: RemoteImageLoader,
) : ViewModel() {

    var state = mutableStateOf(ImagesUiState())
        private set

    fun setCategory(category: String?) {
        state.value = state.value.copy(category = category)
    }

    fun setGroupSeries(value: Boolean) {
        state.value = state.value.copy(groupSeries = value)
    }

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            state.value = try {
                state.value.copy(images = fetchAll(), loading = false)
            } catch (e: Exception) {
                state.value.copy(loading = false, error = e.message ?: "Nie udało się pobrać zdjęć.")
            }
        }
    }

    fun upload(picked: PickedFile, form: ImageUploadForm) {
        val current = state.value
        if (current.uploading) return
        val error = validate(picked, form)
        if (error != null) {
            state.value = current.copy(uploadError = error)
            return
        }
        state.value = current.copy(uploading = true, uploadError = null)
        viewModelScope.launch {
            state.value = try {
                repository.uploadImage(
                    picked = picked,
                    title = form.title.trim().ifEmpty { null },
                    description = form.description.trim().ifEmpty { null },
                    category = form.category,
                    symptomId = form.symptomId.trim().ifEmpty { null },
                    capturedAt = form.capturedAt.trim().ifEmpty { null },
                )
                state.value.copy(
                    images = fetchAll(),
                    uploading = false,
                    uploadCompleted = true,
                    message = "Zdjęcie zostało dodane.",
                )
            } catch (e: Exception) {
                state.value.copy(uploading = false, uploadError = e.message ?: "Nie udało się wysłać zdjęcia.")
            }
        }
    }

    fun consumeUploadCompleted() {
        state.value = state.value.copy(uploadCompleted = false, uploadError = null)
    }

    fun delete(image: ImageDto) {
        if (state.value.busyId != null) return
        state.value = state.value.copy(busyId = image.id, error = null, message = null)
        viewModelScope.launch {
            state.value = try {
                repository.deleteImage(image.id)
                imageLoader.evict(image.id)
                state.value.copy(
                    busyId = null,
                    images = state.value.images.filterNot { it.id == image.id },
                    message = "Zdjęcie zostało usunięte.",
                )
            } catch (e: Exception) {
                state.value.copy(busyId = null, error = e.message ?: "Nie udało się usunąć zdjęcia.")
            }
        }
    }

    private suspend fun fetchAll(): List<ImageDto> = repository.images(page = 0, size = 100).content

    private fun validate(picked: PickedFile, form: ImageUploadForm): String? {
        val captured = form.capturedAt.trim()
        return when {
            picked.sizeBytes <= 0L -> "Nie można odczytać wybranego pliku."
            picked.sizeBytes > PatientCardRepository.MAX_FILE_BYTES ->
                "Plik jest za duży. Maksymalny rozmiar to 25 MB."
            captured.isNotEmpty() && !DATE_TIME_PATTERN.matches(captured) ->
                "Data wykonania musi mieć format RRRR-MM-DD GG:MM."
            else -> null
        }
    }

    companion object {
        val CATEGORIES = listOf("SKIN", "INJURY", "MEDICATION_PACKAGE", "DOCUMENT", "OTHER")

        private val DATE_TIME_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")

        /**
         * Dzieli zdjęcia na serie: wspólny `symptomId` oznacza jedną serię,
         * zdjęcia bez objawu zostają osobno. Serie sortowane od najnowszej.
         */
        fun groupIntoSeries(images: List<ImageDto>): List<ImageSeries> =
            images.groupBy { it.symptomId ?: "single-${it.id}" }
                .map { (key, items) ->
                    val sorted = items.sortedByDescending { it.sortKey() }
                    val first = sorted.first()
                    val title = first.title?.takeIf { it.isNotBlank() } ?: "Zdjęcie #${first.imageNumber}"
                    ImageSeries(
                        key = key,
                        title = if (key.startsWith("single-")) title else "Seria: $title",
                        images = sorted,
                    )
                }
                .sortedByDescending { it.images.first().sortKey() }

        private fun ImageDto.sortKey(): String = capturedAt ?: uploadedAt.orEmpty()
    }
}
