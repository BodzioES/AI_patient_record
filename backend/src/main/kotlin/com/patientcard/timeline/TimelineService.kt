package com.patientcard.timeline

import com.patientcard.common.PageResponse
import com.patientcard.entity.Appointment
import com.patientcard.entity.Measurement
import com.patientcard.entity.MedicalDocument
import com.patientcard.entity.MedicalImage
import com.patientcard.entity.Medication
import com.patientcard.entity.Symptom
import com.patientcard.patient.CurrentPatientService
import com.patientcard.repository.AppointmentRepository
import com.patientcard.repository.MeasurementRepository
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.repository.SymptomRepository
import com.patientcard.security.UserPrincipal
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * Buduje wspólną chronologię zdarzeń z siedmiu sekcji karty pacjenta.
 *
 * Każde źródło jest czytane z limitem [PER_SOURCE] najnowszych pozycji, potem
 * zdarzenia są scalane i sortowane malejąco po dacie. To wystarcza dla
 * prywatnej karty pacjenta (setki wpisów), a nie wymaga ciężkich zapytań UNION.
 */
@Service
class TimelineService(
    private val appointmentRepository: AppointmentRepository,
    private val measurementRepository: MeasurementRepository,
    private val symptomRepository: SymptomRepository,
    private val medicationRepository: MedicationRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val imageRepository: MedicalImageRepository,
    private val currentPatientService: CurrentPatientService,
) {

    @Transactional(readOnly = true)
    fun timeline(
        principal: UserPrincipal,
        from: Instant?,
        to: Instant?,
        types: Set<TimelineEventType>?,
        page: Int,
        size: Int,
    ): PageResponse<TimelineEventResponse> {
        val patientId = currentPatientService.patientId(principal)
        val fromBound = from ?: Instant.EPOCH
        val toBound = to ?: MAX_INSTANT
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val wanted = types?.takeIf { it.isNotEmpty() } ?: TimelineEventType.entries.toSet()

        val events = buildList {
            if (TimelineEventType.APPOINTMENT in wanted) addAll(appointments(patientId, fromBound, toBound))
            if (TimelineEventType.MEASUREMENT in wanted) addAll(measurements(patientId, fromBound, toBound))
            if (TimelineEventType.SYMPTOM in wanted) addAll(symptoms(patientId, fromBound, toBound))
            if (TimelineEventType.MEDICATION in wanted) addAll(medications(patientId, fromBound, toBound))
            if (TimelineEventType.DOCUMENT in wanted) addAll(documents(patientId, fromBound, toBound))
            if (TimelineEventType.IMAGE in wanted) addAll(images(patientId, fromBound, toBound))
        }
            .filter { !it.occurredAt.isBefore(fromBound) && !it.occurredAt.isAfter(toBound) }
            .sortedByDescending { it.occurredAt }

        val total = events.size
        val start = safePage * safeSize
        val content = if (start >= total) emptyList() else events.subList(start, minOf(start + safeSize, total))
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

    private fun appointments(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        appointmentRepository
            .findAllByPatientIdAndScheduledAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "scheduledAt")),
            )
            .content
            .map { it.toEvent() }

    private fun measurements(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        measurementRepository
            .findAllByPatientIdAndMeasuredAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "measuredAt")),
            )
            .content
            .map { it.toEvent() }

    private fun symptoms(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        symptomRepository
            .findAllByPatientIdAndObservedAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "observedAt")),
            )
            .content
            .map { it.toEvent() }

    private fun medications(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        medicationRepository
            .findAllByPatientIdAndCreatedAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "createdAt")),
            )
            .content
            .map { it.toEvent() }

    private fun documents(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        documentRepository
            .findAllByPatientIdAndUploadedAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "uploadedAt")),
            )
            .content
            .map { it.toEvent() }

    private fun images(patientId: UUID, from: Instant, to: Instant): List<TimelineEventResponse> =
        imageRepository
            .findAllByPatientIdAndUploadedAtBetween(
                patientId, from, to,
                PageRequest.of(0, PER_SOURCE, Sort.by(Sort.Direction.DESC, "uploadedAt")),
            )
            .content
            .map { it.toEvent() }

    private fun Appointment.toEvent(): TimelineEventResponse {
        val subtitle = listOfNotNull(
            location?.takeIf { it.isNotBlank() },
            status.takeIf { it.name == "CANCELLED" }?.let { "wizyta odwołana" },
        ).joinToString(" • ")
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.APPOINTMENT,
            occurredAt = scheduledAt,
            title = reason.ifBlank { "Wizyta" },
            subtitle = subtitle.ifBlank { null },
            status = status.name,
            resourceId = id!!,
        )
    }

    private fun Measurement.toEvent(): TimelineEventResponse {
        val formatted = buildString {
            append(value.stripTrailingZeros().toPlainString())
            secondaryValue?.let { append("/").append(it.stripTrailingZeros().toPlainString()) }
            if (unit.isNotBlank()) append(" ").append(unit)
        }
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.MEASUREMENT,
            occurredAt = measuredAt,
            title = "${prettyEnum(type.name)}: $formatted",
            subtitle = notes?.takeIf { it.isNotBlank() },
            status = null,
            resourceId = id!!,
        )
    }

    private fun Symptom.toEvent(): TimelineEventResponse {
        val subtitle = listOfNotNull(
            bodyLocation?.takeIf { it.isNotBlank() },
            severity?.let { "nasilenie $it/10" },
            description?.takeIf { it.isNotBlank() }?.take(120),
        ).joinToString(" • ")
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.SYMPTOM,
            occurredAt = observedAt,
            title = title.ifBlank { "Objaw" },
            subtitle = subtitle.ifBlank { null },
            status = status.name,
            resourceId = id!!,
        )
    }

    private fun Medication.toEvent(): TimelineEventResponse {
        val occurred = startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant() ?: createdAt
        val subtitle = listOfNotNull(
            prescribedDose?.takeIf { it.isNotBlank() },
            frequency?.takeIf { it.isNotBlank() },
            strength?.takeIf { it.isNotBlank() },
        ).joinToString(" • ")
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.MEDICATION,
            occurredAt = occurred,
            title = name.ifBlank { "Lek" },
            subtitle = subtitle.ifBlank { null },
            status = status.name,
            resourceId = id!!,
        )
    }

    private fun MedicalDocument.toEvent(): TimelineEventResponse {
        val subtitle = listOfNotNull(
            prettyEnum(documentType),
            originalFilename.takeIf { it.isNotBlank() },
            documentDate?.let { "dokument z $it" },
        ).joinToString(" • ")
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.DOCUMENT,
            occurredAt = uploadedAt,
            title = title.ifBlank { originalFilename },
            subtitle = subtitle.ifBlank { null },
            status = processingStatus.name,
            resourceId = id!!,
        )
    }

    private fun MedicalImage.toEvent(): TimelineEventResponse {
        val subtitle = listOfNotNull(
            prettyEnum(category.name),
            description?.takeIf { it.isNotBlank() }?.take(120),
        ).joinToString(" • ")
        return TimelineEventResponse(
            id = id!!,
            type = TimelineEventType.IMAGE,
            occurredAt = capturedAt ?: uploadedAt,
            title = title?.takeIf { it.isNotBlank() } ?: "Zdjęcie #$imageNumber",
            subtitle = subtitle.ifBlank { null },
            status = aiProcessingStatus.name,
            resourceId = id!!,
        )
    }

    companion object {
        /** Ile najnowszych pozycji z każdej sekcji bierzemy do scalenia. */
        const val PER_SOURCE = 200

        /** Maksymalny rozmiar strony chronologii. */
        const val MAX_PAGE_SIZE = 100

        private val MAX_INSTANT: Instant = Instant.parse("9999-12-31T23:59:59Z")

        fun prettyEnum(value: String): String = when (value) {
            "BLOOD_PRESSURE" -> "Ciśnienie"
            "HEART_RATE" -> "Tętno"
            "WEIGHT" -> "Waga"
            "TEMPERATURE" -> "Temperatura"
            "GLUCOSE" -> "Glukoza"
            "OXYGEN_SATURATION" -> "Saturacja"
            "LAB_RESULT" -> "Wynik badania"
            "DISCHARGE_SUMMARY" -> "Wypis ze szpitala"
            "PRESCRIPTION" -> "Recepta"
            "REFERRAL" -> "Skierowanie"
            "IMAGING" -> "Badanie obrazowe"
            "SKIN" -> "Skóra"
            "INJURY" -> "Uraz"
            "MEDICATION_PACKAGE" -> "Opakowanie leku"
            "DOCUMENT" -> "Dokument"
            "OTHER" -> "Inne"
            "ACTIVE" -> "Aktywny"
            "RESOLVED" -> "Rozwiązany"
            "SUSPECTED" -> "Podejrzewany"
            "MONITORING" -> "Obserwacja"
            "PAUSED" -> "Wstrzymany"
            "STOPPED" -> "Odstawiony"
            "COMPLETED" -> "Zakończony"
            "SCHEDULED" -> "Zaplanowana"
            "CANCELLED" -> "Odwołana"
            "MISSED" -> "Nieodbyta"
            else -> value.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
