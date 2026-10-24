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
import java.time.Instant
import java.util.UUID

interface DoctorRepository : JpaRepository<Doctor, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Doctor?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Doctor>
}

interface MedicalDocumentRepository : JpaRepository<MedicalDocument, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): MedicalDocument?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<MedicalDocument>
    fun findAllByPatientIdOrderByUploadedAtDesc(patientId: UUID, pageable: Pageable): Page<MedicalDocument>
}

interface MedicalConditionRepository : JpaRepository<MedicalCondition, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): MedicalCondition?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<MedicalCondition>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: ConditionStatus, pageable: Pageable): Page<MedicalCondition>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<MedicalCondition>
}

interface AllergyRepository : JpaRepository<Allergy, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Allergy?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Allergy>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: AllergyStatus, pageable: Pageable): Page<Allergy>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<Allergy>
}

interface SymptomRepository : JpaRepository<Symptom, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Symptom?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Symptom>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: SymptomStatus, pageable: Pageable): Page<Symptom>
    fun findAllByPatientIdOrderByObservedAtDesc(patientId: UUID, pageable: Pageable): List<Symptom>
}

interface MedicationRepository : JpaRepository<Medication, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Medication?
    fun existsByIdAndPatientId(id: UUID, patientId: UUID): Boolean
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Medication>
    fun findAllByPatientIdAndStatus(patientId: UUID, status: MedicationStatus, pageable: Pageable): Page<Medication>
    fun findAllByPatientIdOrderByCreatedAtDesc(patientId: UUID, pageable: Pageable): List<Medication>
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
}

interface MeasurementRepository : JpaRepository<Measurement, UUID> {
    fun findByIdAndPatientId(id: UUID, patientId: UUID): Measurement?
    fun findAllByPatientId(patientId: UUID, pageable: Pageable): Page<Measurement>
    fun findAllByPatientIdAndType(patientId: UUID, type: MeasurementType, pageable: Pageable): Page<Measurement>
    fun findAllByPatientIdOrderByMeasuredAtDesc(patientId: UUID, pageable: Pageable): List<Measurement>
}
