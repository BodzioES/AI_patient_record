package com.patientcard.ai.provider

import com.patientcard.entity.AiSourceType
import java.time.Instant
import java.util.UUID

data class AiHistoryMessage(
    val role: String,
    val content: String,
)

data class AiContextSource(
    val type: AiSourceType,
    val id: UUID,
    val label: String,
    val relevance: String,
)

data class PatientContext(
    val text: String,
    val sources: List<AiContextSource>,
    val truncated: Boolean,
)

data class AiChatCommand(
    val question: String,
    val context: PatientContext,
    val history: List<AiHistoryMessage>,
)

data class AiAnswer(
    val text: String,
    val modelName: String,
    val modelVersion: String?,
)

data class AiImageCommand(
    val imageId: UUID,
    val imageNumber: Long,
    val mimeType: String,
    val title: String?,
    val category: String,
    val capturedAt: Instant?,
    /** Only populated for providers that actually inspect pixels. */
    val imageBytes: ByteArray?,
)

data class AiImageAnswer(
    val text: String,
    val modelName: String,
    val modelVersion: String?,
    val promptVersion: String,
)

/**
 * Abstraction over the local model runtime. ETAP 1 uses MockAiProvider only.
 * A future LM Studio implementation must not change business logic in services.
 */
interface AiProvider {
    val name: String
    val version: String?

    fun isMock(): Boolean

    fun chat(command: AiChatCommand): AiAnswer

    fun analyzeImage(command: AiImageCommand): AiImageAnswer
}
