package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ai_message_sources")
class AiMessageSource(
    @Column(name = "message_id", nullable = false)
    var messageId: UUID = UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    var sourceType: AiSourceType = AiSourceType.SYMPTOM,

    @Column(name = "source_id", nullable = false)
    var sourceId: UUID = UUID(0, 0),

    @Column(name = "relevance_description", length = 2000)
    var relevanceDescription: String? = null,
) : CreatedEntity()
