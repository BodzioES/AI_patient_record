package com.example.ar_project_1.data.repository

import android.content.ContentResolver
import com.example.ar_project_1.data.api.ApiErrorParser
import com.example.ar_project_1.data.api.ApiService
import com.example.ar_project_1.data.api.AppException
import com.example.ar_project_1.data.model.AuthResponse
import com.example.ar_project_1.data.model.ChatRequest
import com.example.ar_project_1.data.model.ChatResponse
import com.example.ar_project_1.data.model.ConversationDetailDto
import com.example.ar_project_1.data.model.ConversationDto
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.model.LoginRequest
import com.example.ar_project_1.data.model.LogoutRequest
import com.example.ar_project_1.data.model.PageDto
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.model.RegisterRequest
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.RecordRequestBuilder
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.data.session.SessionManager
import com.example.ar_project_1.data.upload.ContentUriRequestBody
import com.example.ar_project_1.data.upload.PickedFile
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

class PatientCardRepository(
    private val contentResolver: ContentResolver,
    private val api: ApiService,
    private val sessionManager: SessionManager,
) {

    suspend fun login(email: String, password: String): AuthResponse = call {
        api.login(LoginRequest(email.trim(), password))
    }.also(sessionManager::updateAuth)

    suspend fun register(email: String, password: String, displayName: String): AuthResponse = call {
        api.register(RegisterRequest(email.trim(), password, displayName.trim()))
    }.also(sessionManager::updateAuth)

    suspend fun logout() {
        val refresh = sessionManager.current().refreshToken
        try {
            if (!refresh.isNullOrBlank()) api.logout(LogoutRequest(refresh))
        } catch (_: Exception) {
            // Local logout must work even without connectivity.
        } finally {
            sessionManager.clearAuth()
        }
    }

    suspend fun patient(): PatientDto = call { api.patient() }

    suspend fun updatePatient(values: Map<String, String>): PatientDto = call {
        api.updatePatient(buildPatientRequest(values))
    }

    suspend fun list(
        type: ResourceType,
        page: Int = 0,
        size: Int = 50,
    ): PageDto<JsonObject> = call { api.list(type.endpoint, page, size) }

    suspend fun get(type: ResourceType, id: String): JsonObject = call {
        api.get("${type.endpoint}/$id")
    }

    suspend fun create(type: ResourceType, values: Map<String, String>): JsonObject = call {
        api.create(type.endpoint, RecordRequestBuilder.build(type, values, isUpdate = false))
    }

    suspend fun update(type: ResourceType, id: String, values: Map<String, String>): JsonObject = call {
        api.update("${type.endpoint}/$id", RecordRequestBuilder.build(type, values, isUpdate = true))
    }

    suspend fun delete(type: ResourceType, id: String) = call {
        api.delete("${type.endpoint}/$id")
    }

    suspend fun documents(page: Int = 0, size: Int = 50): PageDto<DocumentDto> = call {
        api.documents(page, size)
    }

    suspend fun uploadDocument(
        picked: PickedFile,
        documentType: String,
        title: String,
        description: String?,
        documentDate: String?,
    ): DocumentDto = call {
        val file = multipartFile(picked)
        api.uploadDocument(
            file = file,
            documentType = documentType.text(),
            title = title.text(),
            description = description?.takeIf { it.isNotBlank() }?.text(),
            documentDate = documentDate?.takeIf { it.isNotBlank() }?.text(),
        )
    }

    suspend fun downloadDocument(id: String): ByteArray = call {
        api.downloadDocument(id).use { it.bytes() }
    }

    suspend fun deleteDocument(id: String) = call { api.delete("app/documents/$id") }

    suspend fun images(page: Int = 0, size: Int = 50): PageDto<ImageDto> = call {
        api.images(page, size)
    }

    suspend fun uploadImage(
        picked: PickedFile,
        title: String?,
        description: String?,
        category: String,
        symptomId: String?,
        capturedAt: String?,
    ): ImageDto = call {
        api.uploadImage(
            file = multipartFile(picked),
            title = title?.takeIf { it.isNotBlank() }?.text(),
            description = description?.takeIf { it.isNotBlank() }?.text(),
            category = category.text(),
            symptomId = symptomId?.takeIf { it.isNotBlank() }?.text(),
            capturedAt = capturedAt?.takeIf { it.isNotBlank() }?.let { DateInputs.toInstantString(it).text() },
        )
    }

    suspend fun downloadImage(id: String): ByteArray = call {
        api.downloadImage(id).use { it.bytes() }
    }

    suspend fun deleteImage(id: String) = call { api.delete("app/images/$id") }

    suspend fun analyzeImage(id: String): JsonObject = call { api.analyzeImage(id) }

    suspend fun chat(message: String, conversationId: String? = null): ChatResponse = call {
        api.chat(ChatRequest(conversationId = conversationId, message = message))
    }

    suspend fun conversations(): PageDto<ConversationDto> = call { api.conversations() }

    suspend fun conversation(id: String): ConversationDetailDto = call { api.conversation(id) }

    suspend fun deleteConversation(id: String) = call { api.deleteConversation(id) }

    private fun multipartFile(picked: PickedFile): MultipartBody.Part {
        val body = ContentUriRequestBody(contentResolver, picked)
        return MultipartBody.Part.createFormData("file", picked.name, body)
    }

    private fun String.text() = toRequestBody(TEXT_PLAIN)

    private fun buildPatientRequest(values: Map<String, String>): JsonObject {
        val json = JsonObject()
        fun text(key: String) {
            val value = values[key]?.trim().orEmpty()
            json.add(key, if (value.isEmpty()) JsonNull.INSTANCE else JsonPrimitive(value))
        }
        text("firstName")
        text("lastName")
        text("notes")

        val date = values["dateOfBirth"]?.trim().orEmpty()
        if (date.isEmpty()) {
            json.add("dateOfBirth", JsonNull.INSTANCE)
        } else {
            if (!DATE_PATTERN.matches(date)) {
                throw IllegalArgumentException("Data urodzenia musi mieć format RRRR-MM-DD.")
            }
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                ?: throw IllegalArgumentException("Data urodzenia jest nieprawidłowa.")
            if (parsed.isAfter(LocalDate.now())) {
                throw IllegalArgumentException("Data urodzenia nie może być w przyszłości.")
            }
            json.add("dateOfBirth", JsonPrimitive(date))
        }

        fun enumValue(key: String) {
            val value = values[key]?.trim().orEmpty()
            json.add(key, if (value.isEmpty()) JsonNull.INSTANCE else JsonPrimitive(value))
        }
        enumValue("sex")
        enumValue("bloodType")

        fun decimal(key: String) {
            val value = values[key]?.trim().orEmpty().replace(',', '.')
            if (value.isEmpty()) {
                json.add(key, JsonNull.INSTANCE)
            } else {
                val number = value.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("Pole musi być liczbą.")
                json.add(key, JsonPrimitive(number))
            }
        }
        decimal("heightCm")
        decimal("weightKg")
        return json
    }

    private suspend fun <T> call(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val parsed = ApiErrorParser.parse(e.code(), body)
            if (e.code() == 401) sessionManager.clearAuth()
            throw parsed
        } catch (e: IOException) {
            throw ApiErrorParser.network(e)
        } catch (e: AppException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw AppException(e.message ?: "Nieprawidłowe dane.", code = "VALIDATION_ERROR", cause = e)
        }
    }

    companion object {
        private val TEXT_PLAIN = "text/plain".toMediaType()
        private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        const val MAX_FILE_BYTES = 25L * 1024 * 1024
    }
}
