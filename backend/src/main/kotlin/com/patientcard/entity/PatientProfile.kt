package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "patient_profiles")
class PatientProfile(
    @Column(name = "user_id", nullable = false, unique = true)
    var userId: UUID = UUID(0, 0),

    @Column(name = "first_name", length = 100)
    var firstName: String? = null,

    @Column(name = "last_name", length = 100)
    var lastName: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    var sex: Sex? = null,

    @Column(name = "height_cm", precision = 5, scale = 1)
    var heightCm: BigDecimal? = null,

    @Column(name = "weight_kg", precision = 5, scale = 1)
    var weightKg: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 16)
    var bloodType: BloodType? = null,

    @Column(length = 10000)
    var notes: String? = null,
) : BaseEntity()
