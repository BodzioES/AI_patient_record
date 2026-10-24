package com.patientcard.repository

import com.patientcard.entity.AiMessageSource
import com.patientcard.entity.AiProcessingJob
import com.patientcard.entity.ChatConversation
import com.patientcard.entity.ChatMessage
import com.patientcard.entity.ImageAiAnalysis
import com.patientcard.entity.MedicalDocumentExtraction
import com.patientcard.entity.MedicalImage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface MedicalImageRepository : JpaRepository<MedicalImage, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): MedicalImage?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<MedicalImage>
    fun findAllByPatientIdAndSymptomId(patientId: UUID, symptomId: UUID, pageable: Pageable): Page<MedicalImage>
    fun findAllByPatientIdOrderByImageNumberAsc(patientId: UUID, pageable: Pageable): List<MedicalImage>
    fun findAllByPatientIdAndUploadedAtBetween(
        patientId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable,
    ): Page<MedicalImage>
    fun countByPatientId(patientId: UUID): Long

    @Query(
        "select i from MedicalImage i where i.patientId = :patientId and (" +
            "lower(coalesce(i.title, '')) like concat('%', :q, '%') or " +
            "lower(coalesce(i.description, '')) like concat('%', :q, '%') or " +
            "lower(i.originalFilename) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<MedicalImage>
}

interface ImageAiAnalysisRepository : JpaRepository<ImageAiAnalysis, UUID> {
    fun findAllByImageIdOrderByCreatedAtDesc(imageId: UUID): List<ImageAiAnalysis>
}

interface MedicalDocumentExtractionRepository : JpaRepository<MedicalDocumentExtraction, UUID> {
    fun findAllByDocumentIdOrderByCreatedAtDesc(documentId: UUID): List<MedicalDocumentExtraction>
}

interface ChatConversationRepository : JpaRepository<ChatConversation, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): ChatConversation?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<ChatConversation>
    fun findAllByPatientIdOrderByUpdatedAtDesc(patientId: UUID, pageable: Pageable): List<ChatConversation>
}

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    fun findAllByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ChatMessage>
    fun findByIdAndConversationId(id: UUID, conversationId: UUID): ChatMessage?
    fun countByConversationId(conversationId: UUID): Long
}

interface AiMessageSourceRepository : JpaRepository<AiMessageSource, UUID> {
    fun findAllByMessageId(messageId: UUID): List<AiMessageSource>
}

interface AiProcessingJobRepository : JpaRepository<AiProcessingJob, UUID>
