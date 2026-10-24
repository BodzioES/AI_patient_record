package com.patientcard.patient

import com.patientcard.common.NotFoundException
import com.patientcard.entity.PatientProfile
import com.patientcard.repository.PatientProfileRepository
import com.patientcard.security.UserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CurrentPatientService(private val patientProfileRepository: PatientProfileRepository) {

    @Transactional(readOnly = true)
    fun requirePatient(principal: UserPrincipal): PatientProfile =
        patientProfileRepository.findByUserId(principal.id)
            ?: throw NotFoundException("Nie znaleziono karty pacjenta dla zalogowanego użytkownika.")

    fun patientId(principal: UserPrincipal): UUID = requirePatient(principal).id!!
}
