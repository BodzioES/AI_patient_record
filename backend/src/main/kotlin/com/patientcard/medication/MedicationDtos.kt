package com.patientcard.medication

import com.patientcard.entity.IntakeStatus
import com.patientcard.entity.Medication
import com.patientcard.entity.MedicationIntakeLog
import com.patientcard.entity.MedicationStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class MedicationRequest(
    @field:NotBlank(message = "Nazwa leku jest wymagana.")
    @field:Size(max = 200, message = "Nazwa jest zbyt długa.")
    val name: String = "",

    @field:Size(max = 200, message = "Substancja czynna jest zbyt długa.")
    val activeIngredient: String? = null,

    @field:Size(max = 50, message = "Moc jest zbyt długa.")
    val strength: String? = null,

    @field:Size(max = 80, message = "Postać jest zbyt długa.")
    val dosageForm: String? = null,

    @field:Size(max = 100, message = "Dawka jest zbyt długa.")
    val prescribedDose: String? = null,

    @field:Size(max = 80, message = "Droga podania jest zbyt długa.")
    val route: String? = null,

    @field:Size(max = 120, message = "Częstotliwość jest zbyt długa.")
    val frequency: String? = null,

    @field:Size(max = 4000, message = "Instrukcje są zbyt długie.")
    val instructions: String? = null,

    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val prescribingDoctorId: UUID? = null,
    val status: MedicationStatus = MedicationStatus.ACTIVE,

    @field:Size(max = 4000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class MedicationResponse(
    val id: UUID,
    val patientId: UUID,
    val name: String,
    val activeIngredient: String?,
    val strength: String?,
    val dosageForm: String?,
    val prescribedDose: String?,
    val route: String?,
    val frequency: String?,
    val instructions: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val prescribingDoctorId: UUID?,
    val status: MedicationStatus,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Medication.toResponse(): MedicationResponse = MedicationResponse(
    id = id!!,
    patientId = patientId,
    name = name,
    activeIngredient = activeIngredient,
    strength = strength,
    dosageForm = dosageForm,
    prescribedDose = prescribedDose,
    route = route,
    frequency = frequency,
    instructions = instructions,
    startDate = startDate,
    endDate = endDate,
    prescribingDoctorId = prescribingDoctorId,
    status = status,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

data class IntakeLogRequest(
    @field:NotNull(message = "ID leku jest wymagane.")
    val medicationId: UUID? = null,

    val scheduledAt: Instant? = null,
    val takenAt: Instant? = null,

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Ilość musi być większa od 0.")
    val amount: BigDecimal? = null,

    @field:Size(max = 40, message = "Jednostka jest zbyt długa.")
    val unit: String? = null,

    val status: IntakeStatus = IntakeStatus.UNKNOWN,

    @field:Size(max = 4000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class IntakeLogResponse(
    val id: UUID,
    val medicationId: UUID,
    val scheduledAt: Instant?,
    val takenAt: Instant?,
    val amount: BigDecimal?,
    val unit: String?,
    val status: IntakeStatus,
    val notes: String?,
    val createdAt: Instant,
)

fun MedicationIntakeLog.toResponse(): IntakeLogResponse = IntakeLogResponse(
    id = id!!,
    medicationId = medicationId,
    scheduledAt = scheduledAt,
    takenAt = takenAt,
    amount = amount,
    unit = unit,
    status = status,
    notes = notes,
    createdAt = createdAt,
)
