package com.patientcard.ai.provider

import com.patientcard.config.AiProperties
import org.springframework.stereotype.Component

/**
 * Deterministic placeholder provider used in ETAP 1. It never inspects image
 * pixels and never calls a language model. Responses explicitly say they are mock.
 */
@Component
class MockAiProvider(private val properties: AiProperties) : AiProvider {

    override val name: String = "mock-ai-provider"
    override val version: String? = "1"

    override fun isMock(): Boolean = true

    override fun chat(command: AiChatCommand): AiAnswer {
        val question = command.question.trim()
        val sourceList = if (command.context.sources.isEmpty()) {
            "Brak zapisanych źródeł w kontekście."
        } else {
            command.context.sources.joinToString("\n") { "- ${it.type} ${it.id}: ${it.label}" }
        }
        val contextPreview = command.context.text
            .lines()
            .take(30)
            .joinToString("\n")
        val text = buildString {
            appendLine("MOCK — to nie jest odpowiedź rzeczywistego modelu AI.")
            appendLine("Nie uruchomiono LM Studio ani żadnego modelu językowego. Poniższa odpowiedź jest demonstracyjna.")
            appendLine()
            appendLine("Pytanie: $question")
            appendLine()
            appendLine("Kontekst przekazany do dostawcy (skrót):")
            appendLine(contextPreview.ifBlank { "(brak kontekstu)" })
            appendLine()
            appendLine("Źródła dostępne w kontekście:")
            append(sourceList)
            appendLine()
            appendLine()
            append("W ETAPIE 1 backend jedynie przygotowuje kontekst i zapisuje historię rozmowy. ")
            append("Rzeczywista analiza zostanie podłączona w ETAPIE 4 przez LM Studio (OpenAI-compatible API).")
        }
        return AiAnswer(text = text, modelName = name, modelVersion = version)
    }

    override fun analyzeImage(command: AiImageCommand): AiImageAnswer {
        val text = buildString {
            appendLine("MOCK — nie wykonano analizy treści zdjęcia.")
            appendLine("Dostawca ${properties.provider} nie jest jeszcze podłączony, więc żaden model nie obejrzał obrazu.")
            appendLine()
            appendLine("Zdjęcie: #${command.imageNumber} (UUID ${command.imageId})")
            appendLine("Kategoria: ${command.category}")
            appendLine("Tytuł: ${command.title ?: "(brak)"}")
            appendLine("Typ MIME: ${command.mimeType}")
            appendLine()
            append("Ten rekord istnieje po to, aby przetestować zapis wyniku i przepływ API. ")
            append("Nie należy traktować go jako opinii medycznej.")
        }
        return AiImageAnswer(
            text = text,
            modelName = name,
            modelVersion = version,
            promptVersion = "mock-image-v1",
        )
    }
}
