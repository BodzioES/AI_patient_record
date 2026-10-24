package com.patientcard.document

import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.DocumentProcessingStatus
import com.patientcard.entity.MedicalDocument
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.security.UserPrincipal
import com.patientcard.storage.FileStorageService
import com.patientcard.storage.FileTypeValidator
import com.patientcard.storage.StoredFile
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DownloadedFile(
    val path: Path,
    val originalFilename: String,
    val mimeType: String,
    val sizeBytes: Long,
)

@Service
class DocumentService(
    private val documentRepository: MedicalDocumentRepository,
    private val currentPatientService: CurrentPatientService,
    private val fileTypeValidator: FileTypeValidator,
    private val fileStorageService: FileStorageService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, pageable: Pageable): PageResponse<DocumentResponse> {
        val patientId = currentPatientService.patientId(principal)
        return PageResponse.from(
            documentRepository.findAllByPatientIdOrderByUploadedAtDesc(patientId, pageable).map { it.toResponse() },
        )
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): DocumentResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun upload(
        principal: UserPrincipal,
        file: MultipartFile,
        documentType: String,
        title: String,
        description: String?,
        documentDate: LocalDate?,
    ): DocumentResponse {
        val patientId = currentPatientService.patientId(principal)
        val type = fileTypeValidator.validateDocument(file)
        val stored = fileStorageService.storeDocument(patientId, file, type)
        fileStorageService.deleteOnRollback(stored.storageKey)
        val document = MedicalDocument(
            patientId = patientId,
            documentType = documentType.trim().uppercase(),
            title = title.trim(),
            description = description,
            originalFilename = sanitizeFilename(file.originalFilename),
            storageKey = stored.storageKey,
            mimeType = type.mimeType,
            fileSizeBytes = stored.sizeBytes,
            sha256Checksum = stored.sha256,
            documentDate = documentDate,
            uploadedAt = Instant.now(),
            processingStatus = DocumentProcessingStatus.PENDING,
        )
        return documentRepository.save(document).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: DocumentUpdateRequest): DocumentResponse {
        val document = findOwned(principal, id)
        document.documentType = request.documentType.trim().uppercase()
        document.title = request.title.trim()
        document.description = request.description
        document.documentDate = request.documentDate
        return document.toResponse()
    }

    @Transactional(readOnly = true)
    fun download(principal: UserPrincipal, id: UUID): DownloadedFile {
        val document = findOwned(principal, id)
        val path = fileStorageService.load(document.storageKey)
        return DownloadedFile(path, document.originalFilename, document.mimeType, document.fileSizeBytes)
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        val document = findOwned(principal, id)
        documentRepository.delete(document)
        fileStorageService.deleteAfterCommit(document.storageKey)
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): MedicalDocument {
        val patientId = currentPatientService.patientId(principal)
        return documentRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono dokumentu.")
    }

    private fun sanitizeFilename(filename: String?): String {
        val base = (filename ?: "document")
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        return base.take(255).ifEmpty { "document" }
    }
}
