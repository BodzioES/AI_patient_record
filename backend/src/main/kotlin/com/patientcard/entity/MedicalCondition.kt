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
@Table(name = "medical_conditions")
class MedicalCondition(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(nullable = false, length = 200)
    var name: String = "",

    @Column(length = 10000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: ConditionStatus = ConditionStatus.ACTIVE,

    @Column(name = "onset_date")
    var onsetDate: LocalDate? = null,

    @Column(name = "resolved_date")
    var resolvedDate: LocalDate? = null,

    @Column(name = "source_document_id")
    var sourceDocumentId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var origin: ConditionOrigin = ConditionOrigin.PATIENT_REPORTED,
) : BaseEntity()
