package com.example.ar_project_1.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.resource.FieldType
import com.example.ar_project_1.data.resource.FormField
import kotlinx.coroutines.launch

data class ProfileUiState(
    val patient: PatientDto? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class ProfileViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ProfileUiState())
        private set

    fun load() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                state.value = ProfileUiState(patient = repository.patient(), loading = false)
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(loading = false, error = e.message ?: "Błąd pobierania profilu.")
            }
        }
    }
}

data class ProfileEditUiState(
    val values: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

class ProfileEditViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(ProfileEditUiState())
        private set

    init {
        viewModelScope.launch {
            try {
                val patient = repository.patient()
                state.value = ProfileEditUiState(values = patient.toFormValues(), loading = false)
            } catch (e: AppException) {
                state.value = state.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(loading = false, error = e.message ?: "Błąd pobierania profilu.")
            }
        }
    }

    fun update(key: String, value: String) {
        state.value = state.value.copy(values = state.value.values + (key to value), error = null)
    }

    fun save() {
        val current = state.value
        if (current.saving) return
        val missing = PROFILE_FIELDS.firstOrNull { it.required && current.values[it.key].isNullOrBlank() }
        if (missing != null) {
            state.value = current.copy(error = "Pole „${missing.label}” jest wymagane.")
            return
        }
        state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                repository.updatePatient(current.values)
                state.value = state.value.copy(saving = false, saved = true)
            } catch (e: AppException) {
                state.value = state.value.copy(saving = false, error = e.message)
            } catch (e: Exception) {
                state.value = state.value.copy(saving = false, error = e.message ?: "Nie udało się zapisać.")
            }
        }
    }

    fun consumeSaved() {
        state.value = state.value.copy(saved = false)
    }

    private fun PatientDto.toFormValues(): Map<String, String> = mapOf(
        "firstName" to (firstName ?: ""),
        "lastName" to (lastName ?: ""),
        "dateOfBirth" to (dateOfBirth ?: ""),
        "sex" to (sex ?: ""),
        "heightCm" to (heightCm?.toString() ?: ""),
        "weightKg" to (weightKg?.toString() ?: ""),
        "bloodType" to (bloodType ?: ""),
        "notes" to (notes ?: ""),
    )

    companion object {
        val PROFILE_FIELDS = listOf(
            FormField("firstName", "Imię", FieldType.TEXT),
            FormField("lastName", "Nazwisko", FieldType.TEXT),
            FormField("dateOfBirth", "Data urodzenia", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField(
                "sex", "Płeć", FieldType.ENUM,
                options = listOf("FEMALE", "MALE", "OTHER", "UNKNOWN"),
            ),
            FormField("heightCm", "Wzrost (cm)", FieldType.DECIMAL),
            FormField("weightKg", "Waga (kg)", FieldType.DECIMAL),
            FormField(
                "bloodType", "Grupa krwi", FieldType.ENUM,
                options = listOf(
                    "A_POSITIVE", "A_NEGATIVE", "B_POSITIVE", "B_NEGATIVE",
                    "AB_POSITIVE", "AB_NEGATIVE", "ZERO_POSITIVE", "ZERO_NEGATIVE", "UNKNOWN",
                ),
            ),
            FormField("notes", "Notatki", FieldType.MULTILINE),
        )
    }
}
