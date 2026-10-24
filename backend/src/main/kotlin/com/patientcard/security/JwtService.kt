package com.patientcard.security

import com.patientcard.config.AppProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(appProperties: AppProperties) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val properties = appProperties.jwt
    private val key: SecretKey

    init {
        val bytes = properties.secret.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size >= 32) {
            "JWT_SECRET must be at least 32 bytes long. Set the JWT_SECRET environment variable."
        }
        key = Keys.hmacShaKeyFor(bytes)
    }

    @PostConstruct
    fun warnAboutDevelopmentSecret() {
        if (properties.secret.startsWith("dev-only")) {
            logger.warn("Using the development JWT secret. Set JWT_SECRET before exposing the backend on a network.")
        }
    }

    fun createAccessToken(userId: UUID, email: String): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuer(properties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.accessTokenTtl)))
            .signWith(key)
            .compact()
    }

    fun parseAccessToken(token: String): AccessTokenClaims? = try {
        val claims: Claims = Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer)
            .build()
            .parseSignedClaims(token)
            .payload
        val id = UUID.fromString(claims.subject)
        val email = claims["email"] as? String ?: return null
        AccessTokenClaims(id, email, claims.expiration.toInstant())
    } catch (_: Exception) {
        null
    }

    fun accessTokenTtlSeconds(): Long = properties.accessTokenTtl.seconds

    fun newRefreshToken(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hashRefreshToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun refreshTokenExpiry(): Instant = Instant.now().plus(properties.refreshTokenTtl)
}

data class AccessTokenClaims(
    val userId: UUID,
    val email: String,
    val expiresAt: Instant,
)
