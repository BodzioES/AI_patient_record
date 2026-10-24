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
@Table(name = "symptoms")
class Symptom(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(length = 10000)
    var description: String? = null,

    @Column(name = "body_location", length = 120)
    var bodyLocation: String? = null,

    var severity: Short? = null,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "ended_at")
    var endedAt: Instant? = null,

    @Column(name = "observed_at", nullable = false)
    var observedAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: SymptomStatus = SymptomStatus.ACTIVE,
) : BaseEntity()
