package com.patientcard.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "patient_image_counters")
class PatientImageCounter(
    @Id
    @Column(name = "patient_id")
    var patientId: UUID = UUID(0, 0),

    @Column(name = "last_image_number", nullable = false)
    var lastImageNumber: Long = 0,
)
