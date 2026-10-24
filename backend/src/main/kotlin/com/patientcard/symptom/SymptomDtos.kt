package com.patientcard.symptom

import com.patientcard.entity.Symptom
import com.patientcard.entity.SymptomStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class SymptomRequest(
    @field:NotBlank(message = "Tytuł objawu jest wymagany.")
    @field:Size(max = 200, message = "Tytuł jest zbyt długi.")
    val title: String = "",

    @field:Size(max = 10_000, message = "Opis jest zbyt długi.")
    val description: String? = null,

    @field:Size(max = 120, message = "Lokalizacja jest zbyt długa.")
    val bodyLocation: String? = null,

    @field:Min(1, message = "Nasilenie musi być w zakresie 1-10.")
    @field:Max(10, message = "Nasilenie musi być w zakresie 1-10.")
    val severity: Short? = null,

    val startedAt: Instant? = null,
    val endedAt: Instant? = null,

    /** Czas obserwacji. Domyślnie czas zapisu. */
    val observedAt: Instant? = null,

    val status: SymptomStatus = SymptomStatus.ACTIVE,
)

data class SymptomResponse(
    val id: UUID,
    val patientId: UUID,
    val title: String,
    val description: String?,
    val bodyLocation: String?,
    val severity: Short?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val observedAt: Instant,
    val status: SymptomStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Symptom.toResponse(): SymptomResponse = SymptomResponse(
    id = id!!,
    patientId = patientId,
    title = title,
    description = description,
    bodyLocation = bodyLocation,
    severity = severity,
    startedAt = startedAt,
    endedAt = endedAt,
    observedAt = observedAt,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
