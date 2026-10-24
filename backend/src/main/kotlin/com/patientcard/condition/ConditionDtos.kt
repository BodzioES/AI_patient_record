package com.patientcard.condition

import com.patientcard.entity.ConditionOrigin
import com.patientcard.entity.ConditionStatus
import com.patientcard.entity.MedicalCondition
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ConditionRequest(
    @field:NotBlank(message = "Nazwa schorzenia jest wymagana.")
    @field:Size(max = 200, message = "Nazwa jest zbyt długa.")
    val name: String = "",

    @field:Size(max = 10_000, message = "Opis jest zbyt długi.")
    val description: String? = null,

    val status: ConditionStatus = ConditionStatus.ACTIVE,
    val onsetDate: LocalDate? = null,
    val resolvedDate: LocalDate? = null,
    val sourceDocumentId: UUID? = null,
    val origin: ConditionOrigin = ConditionOrigin.PATIENT_REPORTED,
)

data class ConditionResponse(
    val id: UUID,
    val patientId: UUID,
    val name: String,
    val description: String?,
    val status: ConditionStatus,
    val onsetDate: LocalDate?,
    val resolvedDate: LocalDate?,
    val sourceDocumentId: UUID?,
    val origin: ConditionOrigin,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun MedicalCondition.toResponse(): ConditionResponse = ConditionResponse(
    id = id!!,
    patientId = patientId,
    name = name,
    description = description,
    status = status,
    onsetDate = onsetDate,
    resolvedDate = resolvedDate,
    sourceDocumentId = sourceDocumentId,
    origin = origin,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
