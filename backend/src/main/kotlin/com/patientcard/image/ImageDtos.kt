package com.patientcard.image

import com.patientcard.entity.AiProcessingStatus
import com.patientcard.entity.ImageCategory
import com.patientcard.entity.MedicalImage
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ImageUpdateRequest(
    @field:Size(max = 200, message = "Tytuł jest zbyt długi.")
    val title: String? = null,

    @field:Size(max = 10_000, message = "Opis jest zbyt długi.")
    val description: String? = null,

    val category: ImageCategory = ImageCategory.OTHER,
    val symptomId: UUID? = null,
    val documentId: UUID? = null,
    val capturedAt: Instant? = null,

    @field:Positive(message = "Numer sekwencji musi być dodatni.")
    val sequenceNumber: Int? = null,
)

data class ImageResponse(
    val id: UUID,
    val patientId: UUID,
    val symptomId: UUID?,
    val documentId: UUID?,
    val imageNumber: Long,
    val title: String?,
    val description: String?,
    val category: ImageCategory,
    val originalFilename: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val widthPx: Int?,
    val heightPx: Int?,
    val sha256Checksum: String,
    val capturedAt: Instant?,
    val uploadedAt: Instant,
    val sequenceNumber: Int?,
    val aiProcessingStatus: AiProcessingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun MedicalImage.toResponse(): ImageResponse = ImageResponse(
    id = id!!,
    patientId = patientId,
    symptomId = symptomId,
    documentId = documentId,
    imageNumber = imageNumber,
    title = title,
    description = description,
    category = category,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes,
    widthPx = widthPx,
    heightPx = heightPx,
    sha256Checksum = sha256Checksum,
    capturedAt = capturedAt,
    uploadedAt = uploadedAt,
    sequenceNumber = sequenceNumber,
    aiProcessingStatus = aiProcessingStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
