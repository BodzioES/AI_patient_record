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
@Table(name = "medical_images")
class MedicalImage(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(name = "symptom_id")
    var symptomId: UUID? = null,

    @Column(name = "document_id")
    var documentId: UUID? = null,

    @Column(name = "image_number", nullable = false)
    var imageNumber: Long = 0,

    @Column(length = 200)
    var title: String? = null,

    @Column(length = 10000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var category: ImageCategory = ImageCategory.OTHER,

    @Column(name = "original_filename", nullable = false, length = 255)
    var originalFilename: String = "",

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    var storageKey: String = "",

    @Column(name = "mime_type", nullable = false, length = 150)
    var mimeType: String = "",

    @Column(name = "file_size_bytes", nullable = false)
    var fileSizeBytes: Long = 0,

    @Column(name = "width_px")
    var widthPx: Int? = null,

    @Column(name = "height_px")
    var heightPx: Int? = null,

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    var sha256Checksum: String = "",

    @Column(name = "captured_at")
    var capturedAt: Instant? = null,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant = Instant.now(),

    @Column(name = "sequence_number")
    var sequenceNumber: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_processing_status", nullable = false, length = 32)
    var aiProcessingStatus: AiProcessingStatus = AiProcessingStatus.PENDING,
) : BaseEntity()
