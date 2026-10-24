package com.patientcard.document

import com.patientcard.entity.DocumentProcessingStatus
import com.patientcard.entity.MedicalDocument
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DocumentUpdateRequest(
    @field:NotBlank(message = "Typ dokumentu jest wymagany.")
    @field:Size(max = 40, message = "Typ dokumentu jest zbyt długi.")
    val documentType: String = "",

    @field:NotBlank(message = "Tytuł dokumentu jest wymagany.")
    @field:Size(max = 200, message = "Tytuł jest zbyt długi.")
    val title: String = "",

    @field:Size(max = 10_000, message = "Opis jest zbyt długi.")
    val description: String? = null,

    @field:PastOrPresent(message = "Data dokumentu nie może być w przyszłości.")
    val documentDate: LocalDate? = null,
)

data class DocumentResponse(
    val id: UUID,
    val patientId: UUID,
    val documentType: String,
    val title: String,
    val description: String?,
    val originalFilename: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val documentDate: LocalDate?,
    val uploadedAt: Instant,
    val processingStatus: DocumentProcessingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun MedicalDocument.toResponse(): DocumentResponse = DocumentResponse(
    id = id!!,
    patientId = patientId,
    documentType = documentType,
    title = title,
    description = description,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes,
    sha256Checksum = sha256Checksum,
    documentDate = documentDate,
    uploadedAt = uploadedAt,
    processingStatus = processingStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
