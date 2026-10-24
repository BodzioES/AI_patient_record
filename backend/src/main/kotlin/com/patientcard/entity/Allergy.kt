package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "allergies")
class Allergy(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var substance: String = "",

    @Column(length = 4000)
    var reaction: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var severity: AllergySeverity = AllergySeverity.UNKNOWN,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AllergyStatus = AllergyStatus.ACTIVE,

    @Column(name = "diagnosed_at")
    var diagnosedAt: LocalDate? = null,

    @Column(length = 4000)
    var notes: String? = null,
) : BaseEntity()
