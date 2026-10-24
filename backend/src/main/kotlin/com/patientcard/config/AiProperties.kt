package com.patientcard.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai")
data class AiProperties(
    /** mock | lmstudio */
    val provider: String = "mock",
    val lmStudioBaseUrl: String = "http://localhost:1234/v1",
    val lmStudioModel: String = "local-model",
    val modelVersion: String? = null,
    /**
     * Wartość przekazywana do LM Studio jako `reasoning_effort` (none|low|medium|high).
     * Puste = nie wysyłaj pola. Modele rozumujące zużywają tokeny na "myślenie",
     * dlatego domyślnie wyłączamy ten etap, żeby odpowiedź mieściła się w budżecie.
     */
    val reasoningEffort: String = "none",
    val connectTimeout: java.time.Duration = java.time.Duration.ofSeconds(10),
    val requestTimeout: java.time.Duration = java.time.Duration.ofMinutes(2),
    val maxContextChars: Int = 8000,
    val maxContextItemsPerCategory: Int = 10,
    /** Budżet znaków całego promptu (reguły + kontekst + historia + pytanie). */
    val maxPromptChars: Int = 14000,
    /** Zdjęcia większe niż ten limit nie są wysyłane do modelu (analiza tylko metadanych). */
    val maxVisionImageBytes: Int = 10_485_760,
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 2048,
)
