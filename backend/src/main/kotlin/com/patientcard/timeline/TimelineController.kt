package com.patientcard.timeline

import com.patientcard.common.PageResponse
import com.patientcard.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/app/timeline")
class TimelineController(private val timelineService: TimelineService) {

    @GetMapping
    fun timeline(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(required = false) types: List<TimelineEventType>?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<TimelineEventResponse> =
        timelineService.timeline(principal, from, to, types?.toSet(), page, size)
}
