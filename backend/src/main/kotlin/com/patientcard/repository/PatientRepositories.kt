package com.patientcard.repository

import com.patientcard.entity.PatientImageCounter
import com.patientcard.entity.PatientProfile
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PatientProfileRepository : JpaRepository<PatientProfile, UUID> {
    fun findByUserId(userId: UUID): PatientProfile?
    fun existsByUserId(userId: UUID): Boolean
}

interface PatientImageCounterRepository : JpaRepository<PatientImageCounter, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PatientImageCounter c where c.patientId = :patientId")
    fun findForUpdate(@Param("patientId") patientId: UUID): PatientImageCounter?
}
