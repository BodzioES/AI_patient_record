package com.patientcard.search

import com.patientcard.common.PageResponse
import com.patientcard.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/app/search")
class SearchController(private val searchService: SearchService) {

    @GetMapping
    fun search(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<SearchHitResponse> = searchService.search(principal, q, page, size)
}
