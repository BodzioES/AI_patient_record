package com.patientcard.ai

import com.patientcard.ai.provider.AiImageCommand
import com.patientcard.ai.provider.AiProvider
import com.patientcard.common.ApiException
import com.patientcard.common.NotFoundException
import com.patientcard.entity.AiJobStatus
import com.patientcard.entity.AiProcessingJob
import com.patientcard.entity.AiProcessingStatus
import com.patientcard.entity.ImageAiAnalysis
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AiProcessingJobRepository
import com.patientcard.repository.ImageAiAnalysisRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.security.UserPrincipal
import com.patientcard.storage.FileStorageService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

class AiProviderFailure(message: String, cause: Throwable? = null) :
    ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_PROVIDER_ERROR", message) {
    init {
        if (cause != null) addSuppressed(cause)
    }
}

@Service
class ImageAiAnalysisService(
    private val provider: AiProvider,
    private val imageRepository: MedicalImageRepository,
    private val analysisRepository: ImageAiAnalysisRepository,
    private val jobRepository: AiProcessingJobRepository,
    private val fileStorageService: FileStorageService,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(noRollbackFor = [AiProviderFailure::class])
    fun analyze(principal: UserPrincipal, imageId: UUID): ImageAnalysisResponse {
        val patientId = currentPatientService.patientId(principal)
        val image = imageRepository.findByIdAndPatientId(imageId, patientId)
            ?: throw NotFoundException("Nie znaleziono zdjęcia.")

        val job = jobRepository.save(
            AiProcessingJob(
                patientId = patientId,
                resourceType = "IMAGE",
                resourceId = image.id!!,
                status = AiJobStatus.RUNNING,
                modelName = provider.name,
                startedAt = Instant.now(),
            ),
        )
        image.aiProcessingStatus = AiProcessingStatus.PROCESSING
        try {
            val bytes = if (provider.isMock()) {
                null
            } else {
                fileStorageService.openStream(image.storageKey).use { it.readBytes() }
            }
            val answer = provider.analyzeImage(
                AiImageCommand(
                    imageId = image.id!!,
                    imageNumber = image.imageNumber,
                    mimeType = image.mimeType,
                    title = image.title,
                    category = image.category.name,
                    capturedAt = image.capturedAt,
                    imageBytes = bytes,
                ),
            )
            val analysis = analysisRepository.save(
                ImageAiAnalysis(
                    imageId = image.id!!,
                    modelName = answer.modelName,
                    modelVersion = answer.modelVersion,
                    promptVersion = answer.promptVersion,
                    analysisText = answer.text,
                    extractedMetadata = null,
                ),
            )
            image.aiProcessingStatus = AiProcessingStatus.COMPLETED
            job.status = AiJobStatus.COMPLETED
            job.completedAt = Instant.now()
            jobRepository.save(job)
            return analysis.toResponse(image.imageNumber)
        } catch (ex: Exception) {
            image.aiProcessingStatus = AiProcessingStatus.FAILED
            job.status = AiJobStatus.FAILED
            job.completedAt = Instant.now()
            job.errorMessage = "AI provider failure: ${ex.javaClass.simpleName}"
            jobRepository.save(job)
            throw AiProviderFailure("Analiza AI nie powiodła się.", ex)
        }
    }

    @Transactional(readOnly = true)
    fun listAnalyses(principal: UserPrincipal, imageId: UUID): ImageAnalysisListResponse {
        val patientId = currentPatientService.patientId(principal)
        val image = imageRepository.findByIdAndPatientId(imageId, patientId)
            ?: throw NotFoundException("Nie znaleziono zdjęcia.")
        val analyses = analysisRepository.findAllByImageIdOrderByCreatedAtDesc(imageId)
        return ImageAnalysisListResponse(
            imageId = image.id!!,
            imageNumber = image.imageNumber,
            analyses = analyses.map { it.toResponse(image.imageNumber) },
            mock = provider.isMock(),
            notice = notice(),
        )
    }

    private fun ImageAiAnalysis.toResponse(imageNumber: Long): ImageAnalysisResponse = ImageAnalysisResponse(
        id = id!!,
        imageId = imageId,
        imageNumber = imageNumber,
        modelName = modelName,
        modelVersion = modelVersion,
        promptVersion = promptVersion,
        analysisText = analysisText,
        createdAt = createdAt,
        mock = provider.isMock(),
        notice = notice(),
    )

    private fun notice(): String = if (provider.isMock()) {
        "Wynik pochodzi z MockAiProvider. Nie wykonano rzeczywistej analizy obrazu ani diagnozy medycznej."
    } else {
        "Wynik pochodzi od lokalnego dostawcy AI i nie zastępuje konsultacji lekarskiej."
    }
}
