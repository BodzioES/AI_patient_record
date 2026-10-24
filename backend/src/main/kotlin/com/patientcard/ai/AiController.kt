package com.patientcard.ai

import com.patientcard.common.PageResponse
import com.patientcard.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/ai")
class AiController(
    private val chatService: AiChatService,
    private val imageAiAnalysisService: ImageAiAnalysisService,
) {

    @PostMapping("/chat")
    fun chat(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChatRequest,
    ): ChatResponse = chatService.chat(principal, request)

    @GetMapping("/status")
    fun status(): AiStatusResponse = chatService.status()

    @GetMapping("/conversations")
    fun conversations(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PageableDefault(size = 20, sort = ["updatedAt"], direction = Sort.Direction.DESC) pageable: Pageable,
    ): PageResponse<ConversationResponse> = chatService.listConversations(principal, pageable)

    @GetMapping("/conversations/{id}")
    fun conversation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ConversationDetailResponse = chatService.getConversation(principal, id)

    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    fun deleteConversation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ) {
        chatService.deleteConversation(principal, id)
    }

    @PostMapping("/images/{imageId}/analyze")
    fun analyzeImage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable imageId: UUID,
    ): ImageAnalysisResponse = imageAiAnalysisService.analyze(principal, imageId)

    @GetMapping("/images/{imageId}/analysis")
    fun imageAnalysis(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable imageId: UUID,
    ): ImageAnalysisListResponse = imageAiAnalysisService.listAnalyses(principal, imageId)
}
