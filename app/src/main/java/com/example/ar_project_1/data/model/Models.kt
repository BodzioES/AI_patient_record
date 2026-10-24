package com.example.ar_project_1.data.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class LogoutRequest(
    val refreshToken: String,
)

data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val enabled: Boolean = true,
    val createdAt: String? = null,
)

data class AuthResponse(
    val accessToken: String = "",
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 0,
    val refreshToken: String = "",
    val user: UserDto = UserDto(),
    val patientId: String = "",
)

data class MeDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val enabled: Boolean = true,
    val createdAt: String? = null,
    val patientId: String = "",
)

data class PatientDto(
    val id: String = "",
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val sex: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bloodType: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class PageDto<T>(
    val content: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
)

data class DocumentDto(
    val id: String = "",
    val patientId: String = "",
    val documentType: String = "",
    val title: String = "",
    val description: String? = null,
    val originalFilename: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0,
    val sha256Checksum: String? = null,
    val documentDate: String? = null,
    val uploadedAt: String? = null,
    val processingStatus: String? = null,
)

data class ImageDto(
    val id: String = "",
    val patientId: String = "",
    val symptomId: String? = null,
    val documentId: String? = null,
    val imageNumber: Long = 0,
    val title: String? = null,
    val description: String? = null,
    val category: String = "OTHER",
    val originalFilename: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val sha256Checksum: String? = null,
    val capturedAt: String? = null,
    val uploadedAt: String? = null,
    val sequenceNumber: Int? = null,
    val aiProcessingStatus: String? = null,
)

data class ChatRequest(
    val conversationId: String? = null,
    val message: String,
)

data class AiSourceDto(
    val type: String = "",
    val sourceId: String = "",
    val relevanceDescription: String? = null,
)

data class ChatResponse(
    val conversationId: String = "",
    val userMessageId: String = "",
    val assistantMessageId: String = "",
    val answer: String = "",
    val modelName: String = "",
    val modelVersion: String? = null,
    val mock: Boolean = false,
    val sources: List<AiSourceDto> = emptyList(),
    val createdAt: String? = null,
)

data class ConversationDto(
    val id: String = "",
    val title: String = "",
    val messageCount: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class MessageDto(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val modelName: String? = null,
    val createdAt: String? = null,
    val sources: List<AiSourceDto> = emptyList(),
)

data class ConversationDetailDto(
    val id: String = "",
    val title: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val messages: List<MessageDto> = emptyList(),
)

data class ApiProblem(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
    val errors: Map<String, String>? = null,
)
