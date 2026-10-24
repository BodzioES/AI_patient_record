package com.patientcard.entity

import com.patientcard.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "medical_document_extractions")
class MedicalDocumentExtraction(
    @Column(name = "document_id", nullable = false)
    var documentId: UUID = UUID(0, 0),

    @Column(name = "extracted_text", length = 50000)
    var extractedText: String? = null,

    @Column(name = "extraction_method", nullable = false, length = 80)
    var extractionMethod: String = "",

    @Column(name = "model_name", length = 150)
    var modelName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    var processingStatus: ExtractionStatus = ExtractionStatus.PENDING,
) : BaseEntity()
