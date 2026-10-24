package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "doctors")
class Doctor(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = "",

    @Column(length = 120)
    var specialty: String? = null,

    @Column(name = "facility_name", length = 200)
    var facilityName: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Column(length = 320)
    var email: String? = null,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 4000)
    var notes: String? = null,
) : BaseEntity()
