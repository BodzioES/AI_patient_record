package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "chat_messages")
class ChatMessage(
    @Column(name = "conversation_id", nullable = false)
    var conversationId: UUID = UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var role: ChatRole = ChatRole.USER,

    @Column(nullable = false, length = 20000)
    var content: String = "",

    @Column(name = "model_name", length = 150)
    var modelName: String? = null,

    /** Comma-separated ids of images attached to a user message. */
    @Column(name = "image_ids", length = 1000)
    var imageIds: String? = null,
) : CreatedEntity() {
    fun attachedImageIds(): List<UUID> = imageIds
        ?.split(',')
        ?.mapNotNull { runCatching { UUID.fromString(it.trim()) }.getOrNull() }
        .orEmpty()
}
