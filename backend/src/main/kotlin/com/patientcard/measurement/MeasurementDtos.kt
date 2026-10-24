package com.patientcard.measurement

import com.patientcard.entity.Measurement
import com.patientcard.entity.MeasurementSource
import com.patientcard.entity.MeasurementType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class MeasurementRequest(
    @field:NotNull(message = "Typ pomiaru jest wymagany.")
    val type: MeasurementType? = null,

    @field:NotNull(message = "Wartość jest wymagana.")
    @field:DecimalMin(value = "0.0", message = "Wartość nie może być ujemna.")
    val value: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Wartość dodatkowa nie może być ujemna.")
    val secondaryValue: BigDecimal? = null,

    @field:NotBlank(message = "Jednostka jest wymagana.")
    @field:Size(max = 30, message = "Jednostka jest zbyt długa.")
    val unit: String = "",

    @field:NotNull(message = "Data pomiaru jest wymagana.")
    val measuredAt: Instant? = null,

    val source: MeasurementSource = MeasurementSource.MANUAL,

    @field:Size(max = 4000, message = "Notatki są zbyt długie.")
    val notes: String? = null,
)

data class MeasurementResponse(
    val id: UUID,
    val patientId: UUID,
    val type: MeasurementType,
    val value: BigDecimal,
    val secondaryValue: BigDecimal?,
    val unit: String,
    val measuredAt: Instant,
    val source: MeasurementSource,
    val notes: String?,
    val createdAt: Instant,
)

fun Measurement.toResponse(): MeasurementResponse = MeasurementResponse(
    id = id!!,
    patientId = patientId,
    type = type,
    value = value,
    secondaryValue = secondaryValue,
    unit = unit,
    measuredAt = measuredAt,
    source = source,
    notes = notes,
    createdAt = createdAt,
)
