package com.patientcard.allergy

import com.patientcard.entity.Allergy
import com.patientcard.entity.AllergySeverity
import com.patientcard.entity.AllergyStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AllergyRequest(
    @field:NotBlank(message = "Substancja jest wymagana.")
    @field:Size(max = 200, message = "Substancja jest zbyt długa.")
    val substance: String = "",

    @field:Size(max = 4000, message = "Reakcja jest zbyt długa.")
    val reaction: String? = null,

    val severity: AllergySeverity = AllergySeverity.UNKNOWN,
    val status: AllergyStatus = AllergyStatus.ACTIVE,

    @field:PastOrPresent(message = "Data diagnozy nie może być w przyszłości.")
    val diagnosedAt: LocalDate? = null,

    @field:Size(max = 4000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class AllergyResponse(
    val id: UUID,
    val patientId: UUID,
    val substance: String,
    val reaction: String?,
    val severity: AllergySeverity,
    val status: AllergyStatus,
    val diagnosedAt: LocalDate?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Allergy.toResponse(): AllergyResponse = AllergyResponse(
    id = id!!,
    patientId = patientId,
    substance = substance,
    reaction = reaction,
    severity = severity,
    status = status,
    diagnosedAt = diagnosedAt,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
