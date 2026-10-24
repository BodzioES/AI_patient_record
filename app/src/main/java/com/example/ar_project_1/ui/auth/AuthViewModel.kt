package com.example.ar_project_1.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.repository.PatientCardRepository
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
)

class AuthViewModel(private val repository: PatientCardRepository) : ViewModel() {

    var state = androidx.compose.runtime.mutableStateOf(AuthUiState())
        private set

    fun setMode(mode: AuthMode) {
        state.value = state.value.copy(mode = mode, error = null, fieldErrors = emptyMap())
    }

    fun setEmail(value: String) {
        state.value = state.value.copy(email = value, error = null)
    }

    fun setPassword(value: String) {
        state.value = state.value.copy(password = value, error = null)
    }

    fun setDisplayName(value: String) {
        state.value = state.value.copy(displayName = value, error = null)
    }

    fun submit() {
        val current = state.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            state.value = current.copy(error = errors.values.first(), fieldErrors = errors)
            return
        }
        state.value = current.copy(loading = true, error = null, fieldErrors = emptyMap())
        viewModelScope.launch {
            try {
                if (current.mode == AuthMode.LOGIN) {
                    repository.login(current.email, current.password)
                } else {
                    repository.register(current.email, current.password, current.displayName)
                }
                state.value = state.value.copy(loading = false)
            } catch (e: AppException) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message,
                    fieldErrors = e.fieldErrors,
                )
            } catch (e: Exception) {
                state.value = state.value.copy(
                    loading = false,
                    error = e.message ?: "Nie udało się zalogować.",
                )
            }
        }
    }

    private fun validate(state: AuthUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (!EMAIL_PATTERN.matches(state.email.trim())) {
            errors["email"] = "Podaj poprawny adres email."
        }
        if (state.password.length < 8) {
            errors["password"] = "Hasło musi mieć co najmniej 8 znaków."
        }
        if (state.mode == AuthMode.REGISTER && state.displayName.trim().isEmpty()) {
            errors["displayName"] = "Nazwa użytkownika jest wymagana."
        }
        return errors
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
