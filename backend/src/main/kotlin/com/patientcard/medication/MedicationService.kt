package com.patientcard.medication

import com.patientcard.common.BadRequestException
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Medication
import com.patientcard.entity.MedicationStatus
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.DoctorRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MedicationService(
    private val medicationRepository: MedicationRepository,
    private val doctorRepository: DoctorRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, status: MedicationStatus?, pageable: Pageable): PageResponse<MedicationResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (status == null) {
            medicationRepository.findAllByPatientId(patientId, pageable)
        } else {
            medicationRepository.findAllByPatientIdAndStatus(patientId, status, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): MedicationResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: MedicationRequest): MedicationResponse {
        val patientId = currentPatientService.patientId(principal)
        validateRequest(patientId, request)
        val medication = Medication(patientId = patientId)
        apply(medication, request)
        return medicationRepository.save(medication).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: MedicationRequest): MedicationResponse {
        val medication = findOwned(principal, id)
        validateRequest(medication.patientId, request)
        apply(medication, request)
        return medication.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        medicationRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Medication {
        val patientId = currentPatientService.patientId(principal)
        return medicationRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono leku.")
    }

    private fun validateRequest(patientId: UUID, request: MedicationRequest) {
        if (request.startDate != null && request.endDate != null && request.endDate < request.startDate) {
            throw BadRequestException("Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.")
        }
        val doctorId = request.prescribingDoctorId
        if (doctorId != null && !doctorRepository.existsByIdAndPatientId(doctorId, patientId)) {
            throw BadRequestException("Wskazany lekarz nie istnieje lub należy do innego pacjenta.")
        }
    }

    private fun apply(medication: Medication, request: MedicationRequest) {
        medication.name = request.name.trim()
        medication.activeIngredient = request.activeIngredient
        medication.strength = request.strength
        medication.dosageForm = request.dosageForm
        medication.prescribedDose = request.prescribedDose
        medication.route = request.route
        medication.frequency = request.frequency
        medication.instructions = request.instructions
        medication.startDate = request.startDate
        medication.endDate = request.endDate
        medication.prescribingDoctorId = request.prescribingDoctorId
        medication.status = request.status
        medication.notes = request.notes
    }
}
