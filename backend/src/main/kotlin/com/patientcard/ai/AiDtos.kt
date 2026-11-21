package com.patientcard.ai

import com.patientcard.entity.AiSourceType
import com.patientcard.entity.ChatRole
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ChatRequest(
    val conversationId: UUID? = null,

    /** May be blank when at least one image is attached. */
    @field:Size(max = 4000, message = "Wiadomość może mieć maksymalnie 4000 znaków.")
    val message: String = "",

    /** Ids of images (already uploaded via /app/images) attached to this message. */
    @field:Size(max = 4, message = "Do jednej wiadomości można dołączyć maksymalnie 4 zdjęcia.")
    val imageIds: List<UUID> = emptyList(),
)

data class AiStatusResponse(
    val mock: Boolean,
    val modelName: String,
    val modelVersion: String?,
    val notice: String,
)

data class AiSourceResponse(
    val type: AiSourceType,
    val sourceId: UUID,
    val relevanceDescription: String?,
)

data class ChatResponse(
    val conversationId: UUID,
    val userMessageId: UUID,
    val assistantMessageId: UUID,
    val answer: String,
    val modelName: String,
    val modelVersion: String?,
    val mock: Boolean,
    val sources: List<AiSourceResponse>,
    val createdAt: Instant,
)

data class ConversationResponse(
    val id: UUID,
    val title: String,
    val messageCount: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class MessageResponse(
    val id: UUID,
    val role: ChatRole,
    val content: String,
    val modelName: String?,
    val createdAt: Instant,
    val sources: List<AiSourceResponse>,
    val imageIds: List<UUID> = emptyList(),
)

data class ConversationDetailResponse(
    val id: UUID,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val messages: List<MessageResponse>,
)

data class ImageAnalysisResponse(
    val id: UUID,
    val imageId: UUID,
    val imageNumber: Long,
    val modelName: String,
    val modelVersion: String?,
    val promptVersion: String,
    val analysisText: String,
    val createdAt: Instant,
    val mock: Boolean,
    val notice: String,
)

data class ImageAnalysisListResponse(
    val imageId: UUID,
    val imageNumber: Long,
    val analyses: List<ImageAnalysisResponse>,
    val mock: Boolean,
    val notice: String,
)
