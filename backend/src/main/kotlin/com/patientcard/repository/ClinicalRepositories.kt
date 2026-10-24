package com.patientcard.repository

import com.patientcard.entity.Allergy
import com.patientcard.entity.AllergyStatus
import com.patientcard.entity.Appointment
import com.patientcard.entity.AppointmentStatus
import com.patientcard.entity.ConditionStatus
import com.patientcard.entity.Doctor
import com.patientcard.entity.Measurement
import com.patientcard.entity.MeasurementType
import com.patientcard.entity.MedicalCondition
import com.patientcard.entity.MedicalDocument
import com.patientcard.entity.Medication
import com.patientcard.entity.MedicationIntakeLog
import com.patientcard.entity.MedicationStatus
import com.patientcard.entity.Symptom
import com.patientcard.entity.SymptomStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface DoctorRepository : JpaRepository<Doctor, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Doctor?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Doctor>

    @Query(
        "select d from Doctor d where d.patientId = :patientId and (" +
            "lower(d.firstName) like concat('%', :q, '%') or " +
            "lower(d.lastName) like concat('%', :q, '%') or " +
            "lower(coalesce(d.specialty, '')) like concat('%', :q, '%') or " +
            "lower(coalesce(d.facilityName, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Doctor>
}

interface MedicalDocumentRepository : JpaRepository<MedicalDocument, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): MedicalDocument?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<MedicalDocument>
    fun findAllByPatientIdOrderByUploadedAtDesc(patientId: UUID, pageable: Pageable): Page<MedicalDocument>
    fun findAllByPatientIdAndUploadedAtBetween(
        patientId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable,
    ): Page<MedicalDocument>

    @Query(
        "select d from MedicalDocument d where d.patientId = :patientId and (" +
            "lower(d.title) like concat('%', :q, '%') or " +
            "lower(coalesce(d.description, '')) like concat('%', :q, '%') or " +
            "lower(d.originalFilename) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<MedicalDocument>
}

interface MedicalConditionRepository : JpaRepository<MedicalCondition, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): MedicalCondition?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<MedicalCondition>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: ConditionStatus, pageable: Pageable): Page<MedicalCondition>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<MedicalCondition>

    @Query(
        "select c from MedicalCondition c where c.patientId = :patientId and (" +
            "lower(c.name) like concat('%', :q, '%') or " +
            "lower(coalesce(c.description, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<MedicalCondition>
}

interface AllergyRepository : JpaRepository<Allergy, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Allergy?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Allergy>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: AllergyStatus, pageable: Pageable): Page<Allergy>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<Allergy>

    @Query(
        "select a from Allergy a where a.patientId = :patientId and (" +
            "lower(a.substance) like concat('%', :q, '%') or " +
            "lower(coalesce(a.reaction, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Allergy>
}

interface SymptomRepository : JpaRepository<Symptom, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Symptom?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Symptom>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: SymptomStatus, pageable: Pageable): Page<Symptom>
    fun findAllByPatientIdOrderByObservedAtDesc(patientId: UUID, pageable: Pageable): List<Symptom>
    fun findAllByPatientIdAndObservedAtBetween(
        patientId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable,
    ): Page<Symptom>

    @Query(
        "select s from Symptom s where s.patientId = :patientId and (" +
            "lower(s.title) like concat('%', :q, '%') or " +
            "lower(coalesce(s.description, '')) like concat('%', :q, '%') or " +
            "lower(coalesce(s.bodyLocation, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Symptom>
}

interface MedicationRepository : JpaRepository<Medication, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Medication?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Medication>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: MedicationStatus, pageable: Pageable): Page<Medication>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<Medication>
    fun findAllByPatientIdAndCreatedAtBetween(
        patientId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable,
    ): Page<Medication>

    @Query(
        "select m from Medication m where m.patientId = :patientId and (" +
            "lower(m.name) like concat('%', :q, '%') or " +
            "lower(coalesce(m.activeIngredient, '')) like concat('%', :q, '%') or " +
            "lower(coalesce(m.instructions, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Medication>
}

interface MedicationIntakeLogRepository : JpaRepository<MedicationIntakeLog, UUID> {
    fun findAllByMedicationId(medicationId: UUID, pageable: Pageable): Page<MedicationIntakeLog>
    fun findByIdAndMedicationId(id: UUID, medicationId: UUID): MedicationIntakeLog?
    fun deleteAllByMedicationId(medicationId: UUID)
}

interface AppointmentRepository : JpaRepository<Appointment, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Appointment?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Appointment>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: AppointmentStatus, pageable: Pageable): Page<Appointment>
    fun findAllByPatientIdAndScheduledAtBetween(patientId: UUID, from: Instant, to: Instant, pageable: Pageable): Page<Appointment>
    fun findAllByPatientIdOrderByScheduledAtDesc(patientId: UUID, pageable: Pageable): List<Appointment>
    fun existsByDoctorId(doctorId: UUID): Boolean

    @Query(
        "select a from Appointment a where a.patientId = :patientId and (" +
            "lower(a.reason) like concat('%', :q, '%') or " +
            "lower(coalesce(a.location, '')) like concat('%', :q, '%') or " +
            "lower(coalesce(a.notes, '')) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Appointment>
}

interface MeasurementRepository : JpaRepository<Measurement, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Measurement?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Measurement>
    fun findAllByPatientIdAndType(patientId: UUID, type: MeasurementType, pageable: Pageable): Page<Measurement>
    fun findAllByPatientIdOrderByMeasuredAtDesc(patientId: UUID, pageable: Pageable): List<Measurement>
    fun findAllByPatientIdAndMeasuredAtBetween(
        patientId: UUID,
        from: Instant,
        to: Instant,
        pageable: Pageable,
    ): Page<Measurement>

    @Query(
        "select m from Measurement m where m.patientId = :patientId and (" +
            "lower(coalesce(m.notes, '')) like concat('%', :q, '%') or " +
            "lower(m.unit) like concat('%', :q, '%'))",
    )
    fun search(patientId: UUID, q: String, pageable: Pageable): List<Measurement>
}
