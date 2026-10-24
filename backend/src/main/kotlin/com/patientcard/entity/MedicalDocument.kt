package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "medical_documents")
class MedicalDocument(
    @Column(name = "patient_id", nullable = false)
    var patientId: UUID = UUID(0, 0),

    @Column(name = "document_type", nullable = false, length = 40)
    var documentType: String = "OTHER",

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(length = 4000)
    var description: String? = null,

    @Column(name = "original_filename", nullable = false, length = 255)
    var originalFilename: String = "",

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    var storageKey: String = "",

    @Column(name = "mime_type", nullable = false, length = 150)
    var mimeType: String = "",

    @Column(name = "file_size_bytes", nullable = false)
    var fileSizeBytes: Long = 0,

    @Column(name = "sha256_checksum", nullable = false, length = 64)
    var sha256Checksum: String = "",

    @Column(name = "document_date")
    var documentDate: LocalDate? = null,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant = Instant.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    var processingStatus: DocumentProcessingStatus = DocumentProcessingStatus.PENDING,
) : BaseEntity()
