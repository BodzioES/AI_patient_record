package com.patientcard.search

import com.patientcard.common.PageResponse
import com.patientcard.entity.Allergy
import com.patientcard.entity.Appointment
import com.patientcard.entity.Doctor
import com.patientcard.entity.Measurement
import com.patientcard.entity.MedicalCondition
import com.patientcard.entity.MedicalDocument
import com.patientcard.entity.MedicalImage
import com.patientcard.entity.Medication
import com.patientcard.entity.Symptom
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AllergyRepository
import com.patientcard.repository.AppointmentRepository
import com.patientcard.repository.DoctorRepository
import com.patientcard.repository.MeasurementRepository
import com.patientcard.repository.MedicalConditionRepository
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.repository.SymptomRepository
import com.patientcard.security.UserPrincipal
import com.patientcard.timeline.TimelineService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Proste wyszukiwanie tekstowe po wszystkich sekcjach karty pacjenta.
 *
 * Każde źródło zwraca maksymalnie [PER_SOURCE] trafień (LIKE, bez indeksu
 * pełnotekstowego), a wyniki są scalane i sortowane: najpierw trafienia w tytuł,
 * potem pozostałe, dalej od najnowszych.
 */
@Service
class SearchService(
    private val allergyRepository: AllergyRepository,
    private val conditionRepository: MedicalConditionRepository,
    private val medicationRepository: MedicationRepository,
    private val symptomRepository: SymptomRepository,
    private val measurementRepository: MeasurementRepository,
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val imageRepository: MedicalImageRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun search(
        principal: UserPrincipal,
        query: String,
        page: Int,
        size: Int,
    ): PageResponse<SearchHitResponse> {
        val q = query.trim().lowercase()
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)
        if (q.length < MIN_QUERY_LENGTH) {
            return PageResponse(emptyList(), safePage, safeSize, 0, 0, true, true)
        }

        val patientId = currentPatientService.patientId(principal)
        val pageable = PageRequest.of(0, PER_SOURCE)

        val scored = buildList {
            allergyRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            conditionRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            medicationRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            symptomRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            measurementRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            doctorRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            appointmentRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            documentRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
            imageRepository.search(patientId, q, pageable).forEach { add(it.toHit(q)) }
        }
            .sortedWith(
                compareByDescending<ScoredHit> { it.score }
                    .thenByDescending { it.hit.occurredAt ?: java.time.Instant.EPOCH },
            )
            .map { it.hit }

        val total = scored.size
        val start = safePage * safeSize
        val content = if (start >= total) emptyList() else scored.subList(start, minOf(start + safeSize, total))
        val totalPages = if (total == 0) 0 else (total + safeSize - 1) / safeSize
        return PageResponse(
            content = content.toList(),
            page = safePage,
            size = safeSize,
            totalElements = total.toLong(),
            totalPages = totalPages,
            first = safePage == 0,
            last = start + safeSize >= total,
        )
    }

    private data class ScoredHit(val hit: SearchHitResponse, val score: Int)

    private fun hit(
        id: UUID,
        type: SearchHitType,
        title: String,
        subtitle: String?,
        occurredAt: java.time.Instant?,
        q: String,
    ): ScoredHit = ScoredHit(
        hit = SearchHitResponse(
            id = id,
            type = type,
            title = title,
            subtitle = subtitle,
            occurredAt = occurredAt,
        ),
        score = if (title.lowercase().contains(q)) 2 else 1,
    )

    private fun Allergy.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.ALLERGY,
        title = substance,
        subtitle = listOfNotNull(
            reaction?.takeIf { it.isNotBlank() },
            "alergia",
        ).joinToString(" • "),
        occurredAt = createdAt,
        q = q,
    )

    private fun MedicalCondition.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.CONDITION,
        title = name,
        subtitle = listOfNotNull(
            description?.takeIf { it.isNotBlank() }?.take(120),
            "schorzenie",
        ).joinToString(" • "),
        occurredAt = createdAt,
        q = q,
    )

    private fun Medication.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.MEDICATION,
        title = name,
        subtitle = listOfNotNull(
            activeIngredient?.takeIf { it.isNotBlank() },
            prescribedDose?.takeIf { it.isNotBlank() },
        ).joinToString(" • ").ifBlank { "lek" },
        occurredAt = createdAt,
        q = q,
    )

    private fun Symptom.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.SYMPTOM,
        title = title,
        subtitle = listOfNotNull(
            bodyLocation?.takeIf { it.isNotBlank() },
            description?.takeIf { it.isNotBlank() }?.take(120),
        ).joinToString(" • ").ifBlank { "objaw" },
        occurredAt = observedAt,
        q = q,
    )

    private fun Measurement.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.MEASUREMENT,
        title = "${TimelineService.prettyEnum(type.name)}: " +
            value.stripTrailingZeros().toPlainString() +
            secondaryValue?.let { "/${it.stripTrailingZeros().toPlainString()}" }.orEmpty() +
            " " + unit,
        subtitle = notes?.takeIf { it.isNotBlank() } ?: "pomiar",
        occurredAt = measuredAt,
        q = q,
    )

    private fun Doctor.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.DOCTOR,
        title = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { "Lekarz" },
        subtitle = listOfNotNull(
            specialty?.takeIf { it.isNotBlank() },
            facilityName?.takeIf { it.isNotBlank() },
        ).joinToString(" • ").ifBlank { "lekarz" },
        occurredAt = createdAt,
        q = q,
    )

    private fun Appointment.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.APPOINTMENT,
        title = reason.ifBlank { "Wizyta" },
        subtitle = listOfNotNull(
            location?.takeIf { it.isNotBlank() },
            "wizyta",
        ).joinToString(" • "),
        occurredAt = scheduledAt,
        q = q,
    )

    private fun MedicalDocument.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.DOCUMENT,
        title = title.ifBlank { originalFilename },
        subtitle = listOfNotNull(
            TimelineService.prettyEnum(documentType),
            originalFilename.takeIf { it.isNotBlank() },
        ).joinToString(" • "),
        occurredAt = uploadedAt,
        q = q,
    )

    private fun MedicalImage.toHit(q: String) = hit(
        id = id!!,
        type = SearchHitType.IMAGE,
        title = title?.takeIf { it.isNotBlank() } ?: "Zdjęcie #$imageNumber",
        subtitle = TimelineService.prettyEnum(category.name),
        occurredAt = capturedAt ?: uploadedAt,
        q = q,
    )

    companion object {
        const val PER_SOURCE = 10
        const val MAX_PAGE_SIZE = 50
        const val MIN_QUERY_LENGTH = 2
    }
}
