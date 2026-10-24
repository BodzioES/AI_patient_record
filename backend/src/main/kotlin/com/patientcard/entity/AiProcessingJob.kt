package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_processing_jobs")
class AiProcessingJob(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(name = "resource_type", nullable = false, length = 40)
    var resourceType: String = "IMAGE",

    @Column(name = "resource_id", nullable = false)
    var resourceId: UUID = UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AiJobStatus = AiJobStatus.PENDING,

    @Column(name = "model_name", length = 150)
    var modelName: String? = null,

    @Column(name = "error_message", length = 1000)
    var errorMessage: String? = null,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,
) : CreatedEntity()
