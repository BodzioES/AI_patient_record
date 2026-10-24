package com.patientcard.auth

import com.patientcard.common.ConflictException
import com.patientcard.common.UnauthorizedException
import com.patientcard.entity.PatientImageCounter
import com.patientcard.entity.PatientProfile
import com.patientcard.entity.RefreshToken
import com.patientcard.entity.User
import com.patientcard.repository.PatientImageCounterRepository
import com.patientcard.repository.PatientProfileRepository
import com.patientcard.repository.RefreshTokenRepository
import com.patientcard.repository.UserRepository
import com.patientcard.security.JwtService
import com.patientcard.security.UserPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Locale

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val imageCounterRepository: PatientImageCounterRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = normalizeEmail(request.email)
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("Konto z tym adresem email już istnieje.")
        }
        val user = userRepository.save(
            User(
                email = email,
                passwordHash = passwordEncoder.encode(request.password),
                displayName = request.displayName.trim(),
            ),
        )
        val profile = patientProfileRepository.save(PatientProfile(userId = user.id!!))
        imageCounterRepository.save(PatientImageCounter(patientId = profile.id!!))
        return issueTokens(user, profile.id!!)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(normalizeEmail(request.email))
        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Nieprawidłowy email lub hasło.")
        }
        if (!user.enabled) {
            throw UnauthorizedException("Konto jest wyłączone.")
        }
        val profile = patientProfileRepository.findByUserId(user.id!!)
            ?: throw UnauthorizedException("Konto nie ma karty pacjenta.")
        return issueTokens(user, profile.id!!)
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthResponse {
        val hash = jwtService.hashRefreshToken(request.refreshToken)
        val stored = refreshTokenRepository.findByTokenHashForUpdate(hash)
            ?: throw UnauthorizedException("Nieprawidłowy refresh token.")
        if (stored.revokedAt != null || stored.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException("Refresh token wygasł lub został unieważniony.")
        }
        val user = userRepository.findById(stored.userId)
            .orElseThrow { UnauthorizedException("Użytkownik nie istnieje.") }
        if (!user.enabled) {
            throw UnauthorizedException("Konto jest wyłączone.")
        }
        val profile = patientProfileRepository.findByUserId(user.id!!)
            ?: throw UnauthorizedException("Konto nie ma karty pacjenta.")

        stored.revokedAt = Instant.now()
        refreshTokenRepository.save(stored)
        return issueTokens(user, profile.id!!)
    }

    @Transactional
    fun logout(request: LogoutRequest) {
        val hash = jwtService.hashRefreshToken(request.refreshToken)
        val stored = refreshTokenRepository.findByTokenHash(hash) ?: return
        if (stored.revokedAt == null) {
            stored.revokedAt = Instant.now()
            refreshTokenRepository.save(stored)
        }
    }

    @Transactional(readOnly = true)
    fun me(principal: UserPrincipal): MeResponse {
        val user = userRepository.findById(principal.id)
            .orElseThrow { UnauthorizedException("Użytkownik nie istnieje.") }
        val profile = patientProfileRepository.findByUserId(principal.id)
            ?: throw UnauthorizedException("Konto nie ma karty pacjenta.")
        return MeResponse(
            id = user.id!!,
            email = user.email,
            displayName = user.displayName,
            enabled = user.enabled,
            createdAt = user.createdAt,
            patientId = profile.id!!,
        )
    }

    private fun issueTokens(user: User, patientId: java.util.UUID): AuthResponse {
        val refresh = jwtService.newRefreshToken()
        refreshTokenRepository.save(
            RefreshToken(
                userId = user.id!!,
                tokenHash = jwtService.hashRefreshToken(refresh),
                expiresAt = jwtService.refreshTokenExpiry(),
            ),
        )
        val access = jwtService.createAccessToken(user.id!!, user.email)
        return AuthResponse(
            accessToken = access,
            expiresInSeconds = jwtService.accessTokenTtlSeconds(),
            refreshToken = refresh,
            user = UserResponse(
                id = user.id!!,
                email = user.email,
                displayName = user.displayName,
                enabled = user.enabled,
                createdAt = user.createdAt,
            ),
            patientId = patientId,
        )
    }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)
}
