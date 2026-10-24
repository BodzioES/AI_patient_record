package com.patientcard.image

import com.patientcard.common.BadRequestException
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.document.DownloadedFile
import com.patientcard.entity.AiProcessingStatus
import com.patientcard.entity.ImageCategory
import com.patientcard.entity.MedicalImage
import com.patientcard.entity.PatientImageCounter
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.repository.PatientImageCounterRepository
import com.patientcard.repository.SymptomRepository
import com.patientcard.security.UserPrincipal
import com.patientcard.storage.FileStorageService
import com.patientcard.storage.FileTypeValidator
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

@Service
class ImageService(
    private val imageRepository: MedicalImageRepository,
    private val counterRepository: PatientImageCounterRepository,
    private val symptomRepository: SymptomRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val currentPatientService: CurrentPatientService,
    private val fileTypeValidator: FileTypeValidator,
    private val fileStorageService: FileStorageService,
) {

    @Transactional(readOnly = true)
    fun list(
        principal: UserPrincipal,
        symptomId: UUID?,
        pageable: Pageable,
    ): PageResponse<ImageResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (symptomId == null) {
            imageRepository.findAllByPatientId(patientId, pageable)
        } else {
            imageRepository.findAllByPatientIdAndSymptomId(patientId, symptomId, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): ImageResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun upload(
        principal: UserPrincipal,
        file: MultipartFile,
        title: String?,
        description: String?,
        category: ImageCategory,
        symptomId: UUID?,
        documentId: UUID?,
        capturedAt: Instant?,
        sequenceNumber: Int?,
    ): ImageResponse {
        val patientId = currentPatientService.patientId(principal)
        validateLinks(patientId, symptomId, documentId)
        val type = fileTypeValidator.validateImage(file)
        val imageNumber = allocateImageNumber(patientId)
        val stored = fileStorageService.storeImage(patientId, file, type)
        fileStorageService.deleteOnRollback(stored.storageKey)
        val image = MedicalImage(
            patientId = patientId,
            symptomId = symptomId,
            documentId = documentId,
            imageNumber = imageNumber,
            title = title?.trim()?.ifEmpty { null },
            description = description,
            category = category,
            originalFilename = sanitizeFilename(file.originalFilename),
            storageKey = stored.storageKey,
            mimeType = type.mimeType,
            fileSizeBytes = stored.sizeBytes,
            sha256Checksum = stored.sha256,
            capturedAt = capturedAt,
            uploadedAt = Instant.now(),
            sequenceNumber = sequenceNumber,
            aiProcessingStatus = AiProcessingStatus.PENDING,
        )
        return imageRepository.save(image).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: ImageUpdateRequest): ImageResponse {
        val image = findOwned(principal, id)
        validateLinks(image.patientId, request.symptomId, request.documentId)
        image.title = request.title?.trim()?.ifEmpty { null }
        image.description = request.description
        image.category = request.category
        image.symptomId = request.symptomId
        image.documentId = request.documentId
        image.capturedAt = request.capturedAt
        image.sequenceNumber = request.sequenceNumber
        return image.toResponse()
    }

    @Transactional(readOnly = true)
    fun download(principal: UserPrincipal, id: UUID): DownloadedFile {
        val image = findOwned(principal, id)
        val path = fileStorageService.load(image.storageKey)
        return DownloadedFile(path, image.originalFilename, image.mimeType, image.fileSizeBytes)
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        val image = findOwned(principal, id)
        imageRepository.delete(image)
        fileStorageService.deleteAfterCommit(image.storageKey)
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): MedicalImage {
        val patientId = currentPatientService.patientId(principal)
        return imageRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono zdjęcia.")
    }

    /**
     * Numbering uses a per-patient counter row locked with SELECT ... FOR UPDATE.
     * The counter never decreases, so numbers are not reused after deletions.
     */
    private fun allocateImageNumber(patientId: UUID): Long {
        val counter = counterRepository.findForUpdate(patientId)
            ?: counterRepository.save(PatientImageCounter(patientId = patientId))
        val next = counter.lastImageNumber + 1
        counter.lastImageNumber = next
        return next
    }

    private fun validateLinks(patientId: UUID, symptomId: UUID?, documentId: UUID?) {
        if (symptomId != null && !symptomRepository.existsByIdAndPatientId(symptomId, patientId)) {
            throw BadRequestException("Wskazany objaw nie istnieje lub należy do innego pacjenta.")
        }
        if (documentId != null && !documentRepository.existsByIdAndPatientId(documentId, patientId)) {
            throw BadRequestException("Wskazany dokument nie istnieje lub należy do innego pacjenta.")
        }
    }

    private fun sanitizeFilename(filename: String?): String {
        val base = (filename ?: "image")
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        return base.take(255).ifEmpty { "image" }
    }
}
