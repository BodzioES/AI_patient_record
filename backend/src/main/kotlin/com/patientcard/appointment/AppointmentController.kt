package com.patientcard.appointment

import com.patientcard.common.PageResponse
import com.patientcard.entity.AppointmentStatus
import com.patientcard.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
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
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/app/appointments")
class AppointmentController(private val appointmentService: AppointmentService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) status: AppointmentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @PageableDefault(size = 20, sort = ["scheduledAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<AppointmentResponse> = appointmentService.list(principal, status, from, to, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): AppointmentResponse =
        appointmentService.get(principal, id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AppointmentRequest,
    ): AppointmentResponse = appointmentService.create(principal, request)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AppointmentRequest,
    ): AppointmentResponse = appointmentService.update(principal, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID) {
        appointmentService.delete(principal, id)
    }
}
