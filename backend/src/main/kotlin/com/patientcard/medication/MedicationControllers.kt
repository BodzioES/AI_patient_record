package com.patientcard.medication

import com.patientcard.common.PageResponse
import com.patientcard.entity.MedicationStatus
import com.patientcard.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/app/medications")
class MedicationController(private val medicationService: MedicationService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) status: MedicationStatus?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<MedicationResponse> = medicationService.list(principal, status, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): MedicationResponse =
        medicationService.get(principal, id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: MedicationRequest,
    ): MedicationResponse = medicationService.create(principal, request)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: MedicationRequest,
    ): MedicationResponse = medicationService.update(principal, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID) {
        medicationService.delete(principal, id)
    }
}

@RestController
@RequestMapping("/api/v1/app/medication-intake")
class IntakeLogController(private val intakeLogService: IntakeLogService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam medicationId: UUID,
        @PageableDefault(size = 20, sort = ["scheduledAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<IntakeLogResponse> = intakeLogService.list(principal, medicationId, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): IntakeLogResponse =
        intakeLogService.get(principal, id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: IntakeLogRequest,
    ): IntakeLogResponse = intakeLogService.create(principal, request)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: IntakeLogRequest,
    ): IntakeLogResponse = intakeLogService.update(principal, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID) {
        intakeLogService.delete(principal, id)
    }
}
