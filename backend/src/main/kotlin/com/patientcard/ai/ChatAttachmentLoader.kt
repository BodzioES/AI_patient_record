package com.patientcard.ai

import com.patientcard.ai.provider.AiChatImage
import com.patientcard.ai.provider.AiProvider
import com.patientcard.common.BadRequestException
import com.patientcard.entity.MedicalImage
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.storage.FileStorageService
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Resolves image ids attached to a chat message. Every id must belong to the
 * current patient; pixels are read only for providers that look at them.
 */
@Component
class ChatAttachmentLoader(
    private val provider: AiProvider,
    private val imageRepository: MedicalImageRepository,
    private val fileStorageService: FileStorageService,
) {

    fun findOwned(patientId: UUID, imageIds: List<UUID>): List<MedicalImage> =
        imageIds.distinct().map { id ->
            imageRepository.findByIdAndPatientId(id, patientId)
                ?: throw BadRequestException("Dołączone zdjęcie nie istnieje lub należy do innego pacjenta.")
        }

    fun toProviderImages(images: List<MedicalImage>): List<AiChatImage> = images.map { image ->
        AiChatImage(
            imageNumber = image.imageNumber,
            mimeType = image.mimeType,
            title = image.title,
            bytes = if (provider.isMock()) {
                null
            } else {
                fileStorageService.openStream(image.storageKey).use { it.readBytes() }
            },
        )
    }
}
