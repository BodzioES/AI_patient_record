package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "image_ai_analyses")
class ImageAiAnalysis(
    @Column(name = "image_id", nullable = false)
    var imageId: UUID = UUID(0, 0),

    @Column(name = "model_name", nullable = false, length = 150)
    var modelName: String = "",

    @Column(name = "model_version", length = 100)
    var modelVersion: String? = null,

    @Column(name = "prompt_version", nullable = false, length = 50)
    var promptVersion: String = "",

    @Column(name = "analysis_text", nullable = false, length = 50000)
    var analysisText: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_metadata", columnDefinition = "jsonb")
    var extractedMetadata: String? = null,
) : CreatedEntity()
