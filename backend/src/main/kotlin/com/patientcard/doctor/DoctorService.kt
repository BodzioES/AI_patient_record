package com.patientcard.doctor

import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.Doctor
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.DoctorRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, pageable: Pageable): PageResponse<DoctorResponse> {
        val patientId = currentPatientService.patientId(principal)
        return PageResponse.from(doctorRepository.findAllByPatientId(patientId, pageable).map { it.toResponse() })
    }

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal, id: UUID): DoctorResponse = findOwned(principal, id).toResponse()

    @Transactional
    fun create(principal: UserPrincipal, request: DoctorRequest): DoctorResponse {
        val patientId = currentPatientService.patientId(principal)
        val doctor = Doctor(patientId = patientId)
        apply(doctor, request)
        return doctorRepository.save(doctor).toResponse()
    }

    @Transactional
    fun update(principal: UserPrincipal, id: UUID, request: DoctorRequest): DoctorResponse {
        val doctor = findOwned(principal, id)
        apply(doctor, request)
        return doctor.toResponse()
    }

    @Transactional
    fun delete(principal: UserPrincipal, id: UUID) {
        doctorRepository.delete(findOwned(principal, id))
    }

    private fun findOwned(principal: UserPrincipal, id: UUID): Doctor {
        val patientId = currentPatientService.patientId(principal)
        return doctorRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono lekarza.")
    }

    private fun apply(doctor: Doctor, request: DoctorRequest) {
        doctor.firstName = request.firstName.trim()
        doctor.lastName = request.lastName.trim()
        doctor.specialty = request.specialty
        doctor.facilityName = request.facilityName
        doctor.phone = request.phone
        doctor.email = request.email
        doctor.address = request.address
        doctor.notes = request.notes
    }
}
