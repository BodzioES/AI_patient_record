package com.patientcard.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "Email jest wymagany.")
    @field:Email(message = "Podaj poprawny adres email.")
    @field:Size(max = 320, message = "Email jest zbyt długi.")
    val email: String = "",

    @field:NotBlank(message = "Hasło jest wymagane.")
    @field:Size(min = 8, max = 72, message = "Hasło musi mieć od 8 do 72 znaków.")
    val password: String = "",

    @field:NotBlank(message = "Nazwa użytkownika jest wymagana.")
    @field:Size(max = 120, message = "Nazwa jest zbyt długa.")
    val displayName: String = "",
)

data class LoginRequest(
    @field:NotBlank(message = "Email jest wymagany.")
    @field:Email(message = "Podaj poprawny adres email.")
    val email: String = "",

    @field:NotBlank(message = "Hasło jest wymagane.")
    val password: String = "",
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token jest wymagany.")
    val refreshToken: String = "",
)

data class LogoutRequest(
    @field:NotBlank(message = "Refresh token jest wymagany.")
    val refreshToken: String = "",
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val enabled: Boolean,
    val createdAt: Instant,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val refreshToken: String,
    val user: UserResponse,
    val patientId: UUID,
)

data class MeResponse(
    val id: UUID,
    val email: String,
    val displayName: String,
    val enabled: Boolean,
    val createdAt: Instant,
    val patientId: UUID,
)
