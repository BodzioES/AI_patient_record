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
@Table(name = "medication_intake_logs")
class MedicationIntakeLog(
    @Column(name = "medication_id", nullable = false)
    var medicationId: UUID = UUID(0, 0),

    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null,

    @Column(name = "taken_at")
    var takenAt: Instant? = null,

    var amount: BigDecimal? = null,

    @Column(length = 40)
    var unit: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var status: IntakeStatus = IntakeStatus.UNKNOWN,

    @Column(length = 4000)
    var notes: String? = null,
) : CreatedEntity()
