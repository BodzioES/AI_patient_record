package com.patientcard.repository

import com.patientcard.entity.RefreshToken
import com.patientcard.entity.User
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshToken?

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    fun revokeAllForUser(@Param("userId") userId: UUID, @Param("now") now: Instant): Int

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff or t.revokedAt is not null")
    fun deleteExpiredOrRevoked(@Param("cutoff") cutoff: Instant): Int
}
