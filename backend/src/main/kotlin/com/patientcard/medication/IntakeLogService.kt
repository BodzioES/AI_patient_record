package com.patientcard.medication

import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.MedicationIntakeLog
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.MedicationIntakeLogRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IntakeLogService(
    private val intakeLogRepository: MedicationIntakeLogRepository,
    private val medicationRepository: MedicationRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, medicationId: UUID, pageable: Pageable): PageResponse<IntakeLogResponse> {
        requireMedication(principal, medicationId)
        return PageResponse.from(
            intakeLogRepository.findAllByMedicationId(medicationId, pageable).map { it.toResponse() },
        )
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): IntakeLogResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: IntakeLogRequest): IntakeLogResponse {
        val medicationId = request.medicationId!!
        requireMedication(principal, medicationId)
        val log = MedicationIntakeLog(medicationId = medicationId)
        apply(log, request)
        return intakeLogRepository.save(log).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: IntakeLogRequest): IntakeLogResponse {
        val log = findOwned(principal, id)
        if (request.medicationId != log.medicationId) {
            throw com.patientcard.common.BadRequestException("Nie można przenieść wpisu do innego leku.")
        }
        apply(log, request)
        return log.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        intakeLogRepository.delete(findOwned(principal, id))
    }

    private fun requireMedication(principal: UserPrincipal, medicationId: UUID) {
        val patientId = currentPatientService.patientId(principal)
        medicationRepository.findByIdAndPatientId(medicationId, patientId)
            ?: throw NotFoundException("Nie znaleziono leku.")
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): MedicationIntakeLog {
        val patientId = currentPatientService.patientId(principal)
        val log = intakeLogRepository.findById(id).orElseThrow { NotFoundException("Nie znaleziono wpisu przyjęcia leku.") }
        medicationRepository.findByIdAndPatientId(log.medicationId, patientId)
            ?: throw NotFoundException("Nie znaleziono wpisu przyjęcia leku.")
        return log
    }

    private fun apply(log: MedicationIntakeLog, request: IntakeLogRequest) {
        log.scheduledAt = request.scheduledAt
        log.takenAt = request.takenAt
        log.amount = request.amount
        log.unit = request.unit
        log.status = request.status
        log.notes = request.notes
    }
}
