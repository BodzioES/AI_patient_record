package com.patientcard.patient

import com.patientcard.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PatientService(
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun get(principal: UserPrincipal): PatientResponse =
        currentPatientService.requirePatient(principal).toResponse()

    @Transactional
    fun update(principal: UserPrincipal, request: UpdatePatientRequest): PatientResponse {
        val patient = currentPatientService.requirePatient(principal)
        patient.firstName = request.firstName?.trim()?.ifEmpty { null }
        patient.lastName = request.lastName?.trim()?.ifEmpty { null }
        patient.dateOfBirth = request.dateOfBirth
        patient.sex = request.sex
        patient.heightCm = request.heightCm
        patient.weightKg = request.weightKg
        patient.bloodType = request.bloodType
        patient.notes = request.notes
        return patient.toResponse()
    }
}
