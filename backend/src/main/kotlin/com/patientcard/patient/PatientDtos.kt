package com.patientcard.patient

import com.patientcard.entity.BloodType
import com.patientcard.entity.PatientProfile
import com.patientcard.entity.Sex
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class UpdatePatientRequest(
    @field:Size(max = 100, message = "Imię jest zbyt długie.")
    val firstName: String? = null,

    @field:Size(max = 100, message = "Nazwisko jest zbyt długie.")
    val lastName: String? = null,

    @field:Past(message = "Data urodzenia musi być w przeszłości.")
    val dateOfBirth: LocalDate? = null,

    val sex: Sex? = null,

    @field:DecimalMin(value = "1.0", message = "Wzrost musi być większy od 0.")
    @field:DecimalMax(value = "299.9", message = "Wzrost jest nieprawidłowy.")
    val heightCm: BigDecimal? = null,

    @field:DecimalMin(value = "1.0", message = "Waga musi być większa od 0.")
    @field:DecimalMax(value = "999.9", message = "Waga jest nieprawidłowa.")
    val weightKg: BigDecimal? = null,

    val bloodType: BloodType? = null,

    @field:Size(max = 10_000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class PatientResponse(
    val id: UUID,
    val firstName: String?,
    val lastName: String?,
    val dateOfBirth: LocalDate?,
    val sex: Sex?,
    val heightCm: BigDecimal?,
    val weightKg: BigDecimal?,
    val bloodType: BloodType?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun PatientProfile.toResponse(): PatientResponse = PatientResponse(
    id = id!!,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    sex = sex,
    heightCm = heightCm,
    weightKg = weightKg,
    bloodType = bloodType,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
