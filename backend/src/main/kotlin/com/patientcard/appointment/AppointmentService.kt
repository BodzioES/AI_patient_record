package com.patientcard.appointment

import com.patientcard.common.BadRequestException
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Appointment
import com.patientcard.entity.AppointmentStatus
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AppointmentRepository
import com.patientcard.repository.DoctorRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val doctorRepository: DoctorRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(
        principal: UserPrincipal,
        status: AppointmentStatus?,
        from: Instant?,
        to: Instant?,
        pageable: Pageable,
    ): PageResponse<AppointmentResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = when {
            from != null && to != null -> {
                if (to < from) throw BadRequestException("Koniec zakresu jest wcześniejszy niż początek.")
                appointmentRepository.findAllByPatientIdAndScheduledAtBetween(patientId, from, to, pageable)
            }
            status != null -> appointmentRepository.findAllByPatientIdAndStatus(patientId, status, pageable)
            else -> appointmentRepository.findAllByPatientId(patientId, pageable)
        }
        return PageResponse.from(page.map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): AppointmentResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: AppointmentRequest): AppointmentResponse {
        val patientId = currentPatientService.patientId(principal)
        validate(patientId, request)
        val appointment = Appointment(patientId = patientId)
        apply(appointment, request)
        return appointmentRepository.save(appointment).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: AppointmentRequest): AppointmentResponse {
        val appointment = findOwned(principal, id)
        validate(appointment.patientId, request)
        apply(appointment, request)
        return appointment.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        appointmentRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Appointment {
        val patientId = currentPatientService.patientId(principal)
        return appointmentRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono wizyty.")
    }

    private fun validate(patientId: UUID, request: AppointmentRequest) {
        val scheduledAt = request.scheduledAt
            ?: throw BadRequestException("Data wizyty jest wymagana.")
        if (request.endedAt != null && request.endedAt < scheduledAt) {
            throw BadRequestException("Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.")
        }
        val doctorId = request.doctorId
        if (doctorId != null && !doctorRepository.existsByIdAndPatientId(doctorId, patientId)) {
            throw BadRequestException("Wskazany lekarz nie istnieje lub należy do innego pacjenta.")
        }
    }

    private fun apply(appointment: Appointment, request: AppointmentRequest) {
        appointment.doctorId = request.doctorId
        appointment.scheduledAt = request.scheduledAt!!
        appointment.endedAt = request.endedAt
        appointment.location = request.location
        appointment.reason = request.reason.trim()
        appointment.status = request.status
        appointment.notes = request.notes
        appointment.recommendations = request.recommendations
    }
}
