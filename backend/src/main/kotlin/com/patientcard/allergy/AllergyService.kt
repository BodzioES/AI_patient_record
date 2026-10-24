package com.patientcard.allergy

import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Allergy
import com.patientcard.entity.AllergyStatus
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AllergyRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AllergyService(
    private val allergyRepository: AllergyRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, status: AllergyStatus?, pageable: Pageable): PageResponse<AllergyResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (status == null) {
            allergyRepository.findAllByPatientId(patientId, pageable)
        } else {
            allergyRepository.findAllByPatientIdAndStatus(patientId, status, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): AllergyResponse =
        findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: AllergyRequest): AllergyResponse {
        val patientId = currentPatientService.patientId(principal)
        val allergy = Allergy(patientId = patientId)
        apply(allergy, request)
        return allergyRepository.save(allergy).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: AllergyRequest): AllergyResponse {
        val allergy = findOwned(principal, id)
        apply(allergy, request)
        return allergy.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        allergyRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Allergy {
        val patientId = currentPatientService.patientId(principal)
        return allergyRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono alergii.")
    }

    private fun apply(allergy: Allergy, request: AllergyRequest) {
        allergy.substance = request.substance.trim()
        allergy.reaction = request.reaction
        allergy.severity = request.severity
        allergy.status = request.status
        allergy.diagnosedAt = request.diagnosedAt
        allergy.notes = request.notes
    }
}
