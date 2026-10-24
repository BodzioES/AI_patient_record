package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "measurements")
class Measurement(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var type: MeasurementType = MeasurementType.OTHER,

    @Column(nullable = false, precision = 12, scale = 3)
    var value: BigDecimal = BigDecimal.ZERO,

    @Column(name = "secondary_value", precision = 12, scale = 3)
    var secondaryValue: BigDecimal? = null,

    @Column(nullable = false, length = 30)
    var unit: String = "",

    @Column(name = "measured_at", nullable = false)
    var measuredAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var source: MeasurementSource = MeasurementSource.MANUAL,

    @Column(length = 4000)
    var notes: String? = null,
) : CreatedEntity()
