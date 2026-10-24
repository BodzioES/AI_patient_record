package com.patientcard.entity

import com.patientcard.common.CreatedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID(0, 0),

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.EPOCH,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
) : CreatedEntity()
