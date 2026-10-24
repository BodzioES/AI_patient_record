package com.patientcard.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val storage: StorageProperties = StorageProperties(),
    val jwt: JwtProperties = JwtProperties(),
    val cors: CorsProperties = CorsProperties(),
)

data class StorageProperties(
    val root: String = "../storage",
    val maxFileSizeBytes: Long = 26_214_400,
)

data class JwtProperties(
    val secret: String = "",
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(30),
    val issuer: String = "patient-card",
)

data class CorsProperties(
    val allowedOrigins: String = "",
) {
    fun origins(): List<String> = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
