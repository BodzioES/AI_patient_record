package com.patientcard.doctor

import com.patientcard.entity.Doctor
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class DoctorRequest(
    @field:NotBlank(message = "Imię lekarza jest wymagane.")
    @field:Size(max = 100, message = "Imię jest zbyt długie.")
    val firstName: String = "",

    @field:NotBlank(message = "Nazwisko lekarza jest wymagane.")
    @field:Size(max = 100, message = "Nazwisko jest zbyt długie.")
    val lastName: String = "",

    @field:Size(max = 120, message = "Specjalizacja jest zbyt długa.")
    val specialty: String? = null,

    @field:Size(max = 200, message = "Nazwa placówki jest zbyt długa.")
    val facilityName: String? = null,

    @field:Size(max = 50, message = "Telefon jest zbyt długi.")
    val phone: String? = null,

    @field:Email(message = "Podaj poprawny email lekarza.")
    @field:Size(max = 320, message = "Email jest zbyt długi.")
    val email: String? = null,

    @field:Size(max = 500, message = "Adres jest zbyt długi.")
    val address: String? = null,

    @field:Size(max = 4000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class DoctorResponse(
    val id: UUID,
    val patientId: UUID,
    val firstName: String,
    val lastName: String,
    val specialty: String?,
    val facilityName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Doctor.toResponse(): DoctorResponse = DoctorResponse(
    id = id!!,
    patientId = patientId,
    firstName = firstName,
    lastName = lastName,
    specialty = specialty,
    facilityName = facilityName,
    phone = phone,
    email = email,
    address = address,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
