package com.patientcard.ai.context

import com.patientcard.entity.AiSourceType
import com.patientcard.repository.AppointmentRepository
import com.patientcard.repository.MeasurementRepository
import com.patientcard.repository.MedicalConditionRepository
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.repository.SymptomRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AiSourceValidator(
    private val imageRepository: MedicalImageRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val symptomRepository: SymptomRepository,
    private val medicationRepository: MedicationRepository,
    private val appointmentRepository: AppointmentRepository,
    private val measurementRepository: MeasurementRepository,
    private val conditionRepository: MedicalConditionRepository,
) {

    /**
     * ai_message_sources.source_id is polymorphic, so ownership cannot be enforced
     * by a database foreign key. It is validated here before a source is persisted.
     */
    fun isOwnedByPatient(patientId: UUID, type: AiSourceType, sourceId: UUID): Boolean = when (type) {
        AiSourceType.IMAGE -> imageRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.DOCUMENT -> documentRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.SYMPTOM -> symptomRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.MEDICATION -> medicationRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.APPOINTMENT -> appointmentRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.MEASUREMENT -> measurementRepository.findByIdAndPatientId(sourceId, patientId) != null
        AiSourceType.CONDITION -> conditionRepository.findByIdAndPatientId(sourceId, patientId) != null
    }
}
