package com.patientcard.appointment

import com.patientcard.entity.Appointment
import com.patientcard.entity.AppointmentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class AppointmentRequest(
    val doctorId: UUID? = null,

    @field:NotNull(message = "Data wizyty jest wymagana.")
    val scheduledAt: Instant? = null,

    val endedAt: Instant? = null,

    @field:Size(max = 300, message = "Miejsce jest zbyt długie.")
    val location: String? = null,

    @field:NotBlank(message = "Powód wizyty jest wymagany.")
    @field:Size(max = 4000, message = "Powód jest zbyt długi.")
    val reason: String = "",

    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @field:Size(max = 10_000, message = "Notatki są zbyt długie.")
    val notes: String? = null,

    @field:Size(max = 10_000, message = "Zalecenia są zbyt długie.")
    val recommendations: String? = null,
)

data class AppointmentResponse(
    val id: UUID,
    val patientId: UUID,
    val doctorId: UUID?,
    val scheduledAt: Instant,
    val endedAt: Instant?,
    val location: String?,
    val reason: String,
    val status: AppointmentStatus,
    val notes: String?,
    val recommendations: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Appointment.toResponse(): AppointmentResponse = AppointmentResponse(
    id = id!!,
    patientId = patientId,
    doctorId = doctorId,
    scheduledAt = scheduledAt,
    endedAt = endedAt,
    location = location,
    reason = reason,
    status = status,
    notes = notes,
    recommendations = recommendations,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
