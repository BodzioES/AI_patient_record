package com.patientcard.patient

import com.patientcard.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/app/patient")
class PatientController(private val patientService: PatientService) {

    @GetMapping
    fun get(@AuthenticationPrincipal principal: UserPrincipal): PatientResponse =
        patientService.get(principal)

    @PutMapping
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdatePatientRequest,
    ): PatientResponse = patientService.update(principal, request)
}
