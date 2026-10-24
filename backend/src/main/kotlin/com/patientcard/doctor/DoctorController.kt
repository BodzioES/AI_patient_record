package com.patientcard.doctor

import com.patientcard.common.PageResponse
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/app/doctors")
class DoctorController(private val doctorService: DoctorService) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PageableDefault(size = 20, sort = ["lastName"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): PageResponse<DoctorResponse> = doctorService.list(principal, pageable)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID): DoctorResponse =
        doctorService.get(principal, id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: DoctorRequest,
    ): DoctorResponse = doctorService.create(principal, request)

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: DoctorRequest,
    ): DoctorResponse = doctorService.update(principal, id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable id: UUID) {
        doctorService.delete(principal, id)
    }
}
