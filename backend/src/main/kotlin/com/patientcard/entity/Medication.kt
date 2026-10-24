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
@Table(name = "medications")
class Medication(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(name = "active_ingredient", length = 200)
    var activeIngredient: String? = null,

    @Column(length = 50)
    var strength: String? = null,

    @Column(name = "dosage_form", length = 80)
    var dosageForm: String? = null,

    @Column(name = "prescribed_dose", length = 100)
    var prescribedDose: String? = null,

    @Column(length = 80)
    var route: String? = null,

    @Column(length = 120)
    var frequency: String? = null,

    @Column(length = 4000)
    var instructions: String? = null,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "prescribing_doctor_id")
    var prescribingDoctorId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: MedicationStatus = MedicationStatus.ACTIVE,

    @Column(length = 4000)
    var notes: String? = null,
) : BaseEntity()
