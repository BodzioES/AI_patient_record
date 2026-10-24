package com.patientcard.symptom

import com.patientcard.common.BadRequestException
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Symptom
import com.patientcard.entity.SymptomStatus
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.SymptomRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SymptomService(
    private val symptomRepository: SymptomRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, status: SymptomStatus?, pageable: Pageable): PageResponse<SymptomResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (status == null) {
            symptomRepository.findAllByPatientId(patientId, pageable)
        } else {
            symptomRepository.findAllByPatientIdAndStatus(patientId, status, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): SymptomResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: SymptomRequest): SymptomResponse {
        val patientId = currentPatientService.patientId(principal)
        validate(request)
        val symptom = Symptom(patientId = patientId)
        apply(symptom, request)
        return symptomRepository.save(symptom).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: SymptomRequest): SymptomResponse {
        val symptom = findOwned(principal, id)
        validate(request)
        apply(symptom, request)
        return symptom.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        symptomRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Symptom {
        val patientId = currentPatientService.patientId(principal)
        return symptomRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono objawu.")
    }

    private fun validate(request: SymptomRequest) {
        if (request.startedAt != null && request.endedAt != null && request.endedAt < request.startedAt) {
            throw BadRequestException("Data zakończenia nie może być wcześniejsza niż data początku.")
        }
    }

    private fun apply(symptom: Symptom, request: SymptomRequest) {
        symptom.title = request.title.trim()
        symptom.description = request.description
        symptom.bodyLocation = request.bodyLocation
        symptom.severity = request.severity
        symptom.startedAt = request.startedAt
        symptom.endedAt = request.endedAt
        symptom.observedAt = request.observedAt ?: Instant.now()
        symptom.status = request.status
    }
}
