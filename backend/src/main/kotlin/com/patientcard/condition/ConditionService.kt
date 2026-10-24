package com.patientcard.condition

import com.patientcard.common.BadRequestException
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.ConditionStatus
import com.patientcard.entity.MedicalCondition
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.MedicalConditionRepository
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ConditionService(
    private val conditionRepository: MedicalConditionRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, status: ConditionStatus?, pageable: Pageable): PageResponse<ConditionResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (status == null) {
            conditionRepository.findAllByPatientId(patientId, pageable)
        } else {
            conditionRepository.findAllByPatientIdAndStatus(patientId, status, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): ConditionResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: ConditionRequest): ConditionResponse {
        val patientId = currentPatientService.patientId(principal)
        validate(request)
        validateDocument(patientId, request.sourceDocumentId)
        val condition = MedicalCondition(patientId = patientId)
        apply(condition, request)
        return conditionRepository.save(condition).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: ConditionRequest): ConditionResponse {
        val condition = findOwned(principal, id)
        validate(request)
        validateDocument(condition.patientId, request.sourceDocumentId)
        apply(condition, request)
        return condition.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        conditionRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): MedicalCondition {
        val patientId = currentPatientService.patientId(principal)
        return conditionRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono schorzenia.")
    }

    private fun validate(request: ConditionRequest) {
        if (request.onsetDate != null && request.resolvedDate != null && request.resolvedDate < request.onsetDate) {
            throw BadRequestException("Data ustąpienia nie może być wcześniejsza niż data początku.")
        }
    }

    private fun validateDocument(patientId: UUID, documentId: UUID?) {
        if (documentId != null && !documentRepository.existsByIdAndPatientId(documentId, patientId)) {
            throw BadRequestException("Wskazany dokument nie istnieje lub należy do innego pacjenta.")
        }
    }

    private fun apply(condition: MedicalCondition, request: ConditionRequest) {
        condition.name = request.name.trim()
        condition.description = request.description
        condition.status = request.status
        condition.onsetDate = request.onsetDate
        condition.resolvedDate = request.resolvedDate
        condition.sourceDocumentId = request.sourceDocumentId
        condition.origin = request.origin
    }
}
