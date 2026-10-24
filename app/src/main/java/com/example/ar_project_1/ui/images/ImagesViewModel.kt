package com.example.ar_project_1.ui.images

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.upload.PickedFile
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImagesUiState(
    val images: List<ImageDto> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val uploadCompleted: Boolean = false,
    val busyId: String? = null,
    val message: String? = null,
)

class ImagesViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ImagesUiState())
        private set

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val page = repository.images(page = 0, size = 100)
                state.value = state.value.copy(images = page.content, loading = false, error = null)
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać zdjęć.",
                )
            }
        }
    }

    fun upload(
        picked: PickedFile,
        title: String,
        description: String,
        category: String,
        symptomId: String,
        capturedAt: String,
    ) {
        val current = state.value
        if (current.uploading) return
        if (picked.sizeBytes <= 0L) {
            state.value = current.copy(uploadError = "Nie można odczytać wybranego pliku.")
            return
        }
        if (picked.sizeBytes > PatientCardRepository.MAX_FILE_BYTES) {
            state.value = current.copy(uploadError = "Plik jest za duży. Maksymalny rozmiar to 25 MB.")
            return
        }
        val captured = capturedAt.trim()
        if (captured.isNotEmpty() && !DATE_TIME_PATTERN.matches(captured)) {
            state.value = current.copy(
                uploadError = "Data wykonania musi mieć format RRRR-MM-DD GG:MM.",
            )
            return
        }

        state.value = current.copy(uploading = true, uploadError = null)
        viewModelScope.launch {
            try {
                repository.uploadImage(
                    picked = picked,
                    title = title.trim().takeIf { it.isNotEmpty() },
                    description = description.trim().takeIf { it.isNotEmpty() },
                    category = category,
                    symptomId = symptomId.trim().takeIf { it.isNotEmpty() },
                    capturedAt = captured.takeIf { it.isNotEmpty() },
                )
                val page = repository.images(page = 0, size = 100)
                state.value = state.value.copy(
                    images = page.content,
                    uploading = false,
                    uploadCompleted = true,
                    message = "Zdjęcie zostało dodane.",
                )
            } catch (e: AppException) {
                state.value = state.value.copy(uploading = false, uploadError = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    uploading = false,
                    uploadError = e.message ?: "Nie udało się wysłać zdjęcia.",
                )
            }
        }
    }

    fun consumeUploadCompleted() {
        state.value = state.value.copy(uploadCompleted = false)
    }

    fun delete(image: ImageDto) {
        if (state.value.busyId != null) return
        state.value = state.value.copy(busyId = image.id, error = null, message = null)
        viewModelScope.launch {
            try {
                repository.deleteImage(image.id)
                state.value = state.value.copy(
                    busyId = null,
                    images = state.value.images.filterNot { it.id == image.id },
                    message = "Zdjęcie zostało usunięte.",
                )
            } catch (e: AppException) {
                state.value = state.value.copy(busyId = null, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    busyId = null,
                    error = e.message ?: "Nie udało się usunąć zdjęcia.",
                )
            }
        }
    }

    companion object {
        val CATEGORIES = listOf("SKIN", "INJURY", "MEDICATION_PACKAGE", "DOCUMENT", "OTHER")

        private val DATE_TIME_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")
    }
}

data class ImageAnalysisUi(
    val text: String,
    val modelName: String,
    val modelVersion: String?,
    val mock: Boolean,
    val notice: String,
)

data class ImageDetailUiState(
    val image: ImageBitmap? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val analyzing: Boolean = false,
    val analysisError: String? = null,
    val analysis: ImageAnalysisUi? = null,
)

class ImageDetailViewModel(
    private val repository: PatientCardRepository,
    private val imageId: String,
) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ImageDetailUiState())
        private set

    init {
        loadImage()
    }

    fun loadImage() {
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val bytes = repository.downloadImage(imageId)
                val bitmap = withContext(Dispatchers.Default) {
                    decodeSampled(bytes)?.asImageBitmap()
                }
                if (bitmap == null) {
                    state.value = state.value.copy(
                        loading = false,
                        error = "Nie można zdekodować obrazu.",
                    )
                } else {
                    state.value = state.value.copy(image = bitmap, loading = false)
                }
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się pobrać zdjęcia.",
                )
            }
        }
    }

    fun analyze() {
        if (state.value.analyzing) return
        state.value = state.value.copy(analyzing = true, analysisError = null)
        viewModelScope.launch {
            try {
                val json = repository.analyzeImage(imageId)
                val analysis = json.toAnalysis()
                state.value = state.value.copy(analyzing = false, analysis = analysis)
            } catch (e: AppException) {
                state.value = state.value.copy(analyzing = false, analysisError = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(
                    analyzing = false,
                    analysisError = e.message ?: "Nie udało się wykonać analizy.",
                )
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
            notice = text("notice")
                ?: "Wynik demonstracyjny. To nie jest diagnoza medyczna.",
        )
    }
}

/**
 * Decodes a gallery photo with subsampling so a 12 MP JPEG does not allocate a
 * 48 MB bitmap on low-memory devices.
 */
private fun decodeSampled(bytes: ByteArray): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > MAX_DECODE_SIDE || bounds.outHeight / sample > MAX_DECODE_SIDE) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private const val MAX_DECODE_SIDE = 1600
