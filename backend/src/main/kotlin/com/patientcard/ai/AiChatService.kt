package com.patientcard.ai

import com.patientcard.ai.context.AiSourceValidator
import com.patientcard.ai.context.PatientContextProvider
import com.patientcard.ai.provider.AiChatCommand
import com.patientcard.ai.provider.AiHistoryMessage
import com.patientcard.ai.provider.AiProvider
import com.patientcard.common.NotFoundException
import com.patientcard.common.PageResponse
import com.patientcard.entity.AiMessageSource
import com.patientcard.entity.ChatConversation
import com.patientcard.entity.ChatMessage
import com.patientcard.entity.ChatRole
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AiMessageSourceRepository
import com.patientcard.repository.ChatConversationRepository
import com.patientcard.repository.ChatMessageRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AiChatService(
    private val provider: AiProvider,
    private val contextProvider: PatientContextProvider,
    private val sourceValidator: AiSourceValidator,
    private val conversationRepository: ChatConversationRepository,
    private val messageRepository: ChatMessageRepository,
    private val sourceRepository: AiMessageSourceRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional
    fun chat(principal: UserPrincipal, request: ChatRequest): ChatResponse {
        val patientId = currentPatientService.patientId(principal)
        val conversation = if (request.conversationId == null) {
            conversationRepository.save(
                ChatConversation(
                    patientId = patientId,
                    title = request.message.trim().take(80).ifBlank { "Nowa rozmowa" },
                ),
            )
        } else {
            conversationRepository.findByIdAndPatientId(request.conversationId, patientId)
                ?: throw NotFoundException("Nie znaleziono rozmowy.")
        }

        val history = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversation.id!!)
            .takeLast(HISTORY_LIMIT)
            .map { AiHistoryMessage(role = it.role.name, content = it.content) }
        val context = contextProvider.buildContext(patientId, request.message)
        val answer = provider.chat(
            AiChatCommand(
                question = request.message.trim(),
                context = context,
                history = history,
            ),
        )

        val userMessage = messageRepository.save(
            ChatMessage(
                conversationId = conversation.id!!,
                role = ChatRole.USER,
                content = request.message.trim(),
            ),
        )
        val assistantMessage = messageRepository.save(
            ChatMessage(
                conversationId = conversation.id!!,
                role = ChatRole.ASSISTANT,
                content = answer.text,
                modelName = answer.modelName,
            ),
        )

        val validSources = context.sources.filter {
            sourceValidator.isOwnedByPatient(patientId, it.type, it.id)
        }
        val savedSources = sourceRepository.saveAll(
            validSources.map {
                AiMessageSource(
                    messageId = assistantMessage.id!!,
                    sourceType = it.type,
                    sourceId = it.id,
                    relevanceDescription = "${it.label} — ${it.relevance}".take(2000),
                )
            },
        )
        conversation.updatedAt = Instant.now()
        conversationRepository.save(conversation)

        return ChatResponse(
            conversationId = conversation.id!!,
            userMessageId = userMessage.id!!,
            assistantMessageId = assistantMessage.id!!,
            answer = answer.text,
            modelName = answer.modelName,
            modelVersion = answer.modelVersion,
            mock = provider.isMock(),
            sources = savedSources.map { it.toSourceResponse() },
            createdAt = assistantMessage.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun status(): AiStatusResponse = AiStatusResponse(
        mock = provider.isMock(),
        modelName = provider.name,
        modelVersion = provider.version,
        notice = if (provider.isMock()) {
            "Tryb demonstracyjny (MOCK): odpowiedzi nie pochodzą z modelu językowego."
        } else {
            "Odpowiedzi generuje model lokalny ${provider.name}. To nie jest porada medyczna."
        },
    )

    @Transactional(readOnly = true)
    fun listConversations(principal: UserPrincipal, pageable: Pageable): PageResponse<ConversationResponse> {
        val patientId = currentPatientService.patientId(principal)
        val page = conversationRepository.findAllByPatientId(patientId, pageable)
        return PageResponse.from(
            page.map {
                ConversationResponse(
                    id = it.id!!,
                    title = it.title,
                    messageCount = messageRepository.countByConversationId(it.id!!),
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun getConversation(principal: UserPrincipal, id: UUID): ConversationDetailResponse {
        val patientId = currentPatientService.patientId(principal)
        val conversation = conversationRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono rozmowy.")
        val messages = messageRepository.findAllByConversationIdOrderByCreatedAtAsc(id)
        return ConversationDetailResponse(
            id = conversation.id!!,
            title = conversation.title,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            messages = messages.map { message ->
                MessageResponse(
                    id = message.id!!,
                    role = message.role,
                    content = message.content,
                    modelName = message.modelName,
                    createdAt = message.createdAt,
                    sources = sourceRepository.findAllByMessageId(message.id!!).map { it.toSourceResponse() },
                )
            },
        )
    }

    @Transactional
    fun deleteConversation(principal: UserPrincipal, id: UUID) {
        val patientId = currentPatientService.patientId(principal)
        val conversation = conversationRepository.findByIdAndPatientId(id, patientId)
            ?: throw NotFoundException("Nie znaleziono rozmowy.")
        conversationRepository.delete(conversation)
    }

    private fun AiMessageSource.toSourceResponse(): AiSourceResponse = AiSourceResponse(
        type = sourceType,
        sourceId = sourceId,
        relevanceDescription = relevanceDescription,
    )

    companion object {
        private const val HISTORY_LIMIT = 20
    }
}
