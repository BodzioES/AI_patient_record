package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "appointments")
class Appointment(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(name = "doctor_id")
    var doctorId: UUID? = null,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant = Instant.now(),

    @Column(name = "ended_at")
    var endedAt: Instant? = null,

    @Column(length = 300)
    var location: String? = null,

    @Column(nullable = false, length = 4000)
    var reason: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AppointmentStatus = AppointmentStatus.SCHEDULED,

    @Column(length = 10000)
    var notes: String? = null,

    @Column(length = 10000)
    var recommendations: String? = null,
) : BaseEntity()
