package com.patientcard.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai")
data class AiProperties(
    /** mock | lmstudio (lmstudio will be implemented in ETAP 4). */
    val provider: String = "mock",
    val lmStudioBaseUrl: String = "http://localhost:1234/v1",
    val lmStudioModel: String = "local-model",
    val modelVersion: String? = null,
    val connectTimeout: java.time.Duration = java.time.Duration.ofSeconds(10),
    val requestTimeout: java.time.Duration = java.time.Duration.ofMinutes(2),
    val maxContextChars: Int = 8000,
    val maxContextItemsPerCategory: Int = 10,
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 800,
)
