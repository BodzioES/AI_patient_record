package com.patientcard.measurement

import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Measurement
import com.patientcard.entity.MeasurementType
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.MeasurementRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MeasurementService(
    private val measurementRepository: MeasurementRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(
        principal: UserPrincipal,
        type: MeasurementType?,
        pageable: Pageable,
    ): PageResponse<MeasurementResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = if (type == null) {
            measurementRepository.findAllByPatientId(patientId, pageable)
        } else {
            measurementRepository.findAllByPatientIdAndType(patientId, type, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): MeasurementResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: MeasurementRequest): MeasurementResponse {
        val patientId = currentPatientService.patientId(principal)
        val measurement = Measurement(patientId = patientId)
        apply(measurement, request)
        return measurementRepository.save(measurement).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: MeasurementRequest): MeasurementResponse {
        val measurement = findOwned(principal, id)
        apply(measurement, request)
        return measurement.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        measurementRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Measurement {
        val patientId = currentPatientService.patientId(principal)
        return measurementRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono pomiaru.")
    }

    private fun apply(measurement: Measurement, request: MeasurementRequest) {
        measurement.type = request.type ?: MeasurementType.OTHER
        measurement.value = request.value ?: java.math.BigDecimal.ZERO
        measurement.secondaryValue = request.secondaryValue
        measurement.unit = request.unit.trim()
        measurement.measuredAt = request.measuredAt ?: java.time.Instant.now()
        measurement.source = request.source
        measurement.notes = request.notes
    }
}
