package com.patientcard.ai.context

import com.patientcard.ai.provider.AiContextSource
import com.patientcard.ai.provider.PatientContext
import com.patientcard.config.AiProperties
import com.patientcard.entity.AiSourceType
import com.patientcard.entity.PatientProfile
import com.patientcard.repository.AllergyRepository
import com.patientcard.repository.AppointmentRepository
import com.patientcard.repository.DoctorRepository
import com.patientcard.repository.MeasurementRepository
import com.patientcard.repository.MedicalConditionRepository
import com.patientcard.repository.MedicalDocumentRepository
import com.patientcard.repository.MedicalImageRepository
import com.patientcard.repository.MedicationRepository
import com.patientcard.repository.PatientProfileRepository
import com.patientcard.repository.SymptomRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Builds a deliberately small, question-specific patient context. It uses
 * deterministic keyword routing and precise repository queries; semantic
 * search / OCR is intentionally deferred to later stages.
 */
@Service
class DefaultPatientContextProvider(
    private val patientProfileRepository: PatientProfileRepository,
    private val allergyRepository: AllergyRepository,
    private val medicationRepository: MedicationRepository,
    private val symptomRepository: SymptomRepository,
    private val measurementRepository: MeasurementRepository,
    private val appointmentRepository: AppointmentRepository,
    private val doctorRepository: DoctorRepository,
    private val conditionRepository: MedicalConditionRepository,
    private val documentRepository: MedicalDocumentRepository,
    private val imageRepository: MedicalImageRepository,
    private val properties: AiProperties,
) : PatientContextProvider {

    private enum class Category {
        ALLERGIES, MEDICATIONS, SYMPTOMS, MEASUREMENTS, APPOINTMENTS, CONDITIONS, DOCUMENTS, IMAGES,
    }

    @Transactional(readOnly = true)
    override fun buildContext(patientId: UUID, question: String): PatientContext {
        val profile = patientProfileRepository.findById(patientId).orElse(null)
        val categories = detectCategories(question.lowercase())
        val builder = StringBuilder()
        val sources = mutableListOf<AiContextSource>()
        var truncated = false
        val limit = properties.maxContextItemsPerCategory

        fun append(text: String) {
            if (builder.length + text.length > properties.maxContextChars) {
                truncated = true
                return
            }
            builder.append(text)
        }

        fun addSource(type: AiSourceType, id: UUID, label: String, relevance: String) {
            sources += AiContextSource(type, id, label.take(200), relevance.take(300))
        }

        append("=== KARTA PACJENTA (kontekst wygenerowany przez backend) ===\n")
        if (profile != null) append(profileLine(profile))
        append("\n")

        if (Category.ALLERGIES in categories) {
            val allergies = allergyRepository.findAllByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, limit))
            if (allergies.isNotEmpty()) {
                append("ALERGIE:\n")
                allergies.forEach { allergy ->
                    append(
                        "- ${allergy.substance} | nasilenie: ${allergy.severity} | status: ${allergy.status}" +
                            (allergy.reaction?.let { " | reakcja: $it" } ?: "") + "\n",
                    )
                }
                append("\n")
            }
        }

        if (Category.MEDICATIONS in categories) {
            val medications = medicationRepository.findAllByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, limit))
            if (medications.isNotEmpty()) {
                append("LEKI:\n")
                medications.forEach { med ->
                    append(
                        "- ${med.name}" +
                            (med.strength?.let { " $it" } ?: "") +
                            (med.prescribedDose?.let { " | dawka: $it" } ?: "") +
                            (med.frequency?.let { " | częstość: $it" } ?: "") +
                            " | status: ${med.status}" +
                            (med.startDate?.let { " | od: $it" } ?: "") +
                            (med.endDate?.let { " | do: $it" } ?: "") +
                            (med.instructions?.let { " | instrukcje: $it" } ?: "") + "\n",
                    )
                    addSource(
                        AiSourceType.MEDICATION,
                        med.id!!,
                        "Lek: ${med.name}${med.strength?.let { " $it" } ?: ""}",
                        "Lek pobrany precyzyjnym zapytaniem po patient_id.",
                    )
                }
                append("\n")
            }
        }

        if (Category.SYMPTOMS in categories) {
            val symptoms = symptomRepository.findAllByPatientIdOrderByObservedAtDesc(patientId, PageRequest.of(0, limit))
            if (symptoms.isNotEmpty()) {
                append("OBJAWY:\n")
                symptoms.forEach { symptom ->
                    append(
                        "- ${symptom.title} | status: ${symptom.status}" +
                            (symptom.severity?.let { " | nasilenie: $it/10" } ?: "") +
                            (symptom.bodyLocation?.let { " | lokalizacja: $it" } ?: "") +
                            " | obserwacja: ${formatInstant(symptom.observedAt)}" +
                            (symptom.description?.let { " | opis: $it" } ?: "") + "\n",
                    )
                    addSource(
                        AiSourceType.SYMPTOM,
                        symptom.id!!,
                        "Objaw: ${symptom.title}",
                        "Objaw pacjenta użyty w kontekście.",
                    )
                }
                append("\n")
            }
        }

        if (Category.MEASUREMENTS in categories) {
            val measurements = measurementRepository.findAllByPatientIdOrderByMeasuredAtDesc(patientId, PageRequest.of(0, limit))
            if (measurements.isNotEmpty()) {
                append("POMIARY:\n")
                measurements.forEach { m ->
                    append(
                        "- ${m.type}: ${m.value}${m.secondaryValue?.let { "/$it" } ?: ""} ${m.unit}" +
                            " | ${formatInstant(m.measuredAt)} | źródło: ${m.source}\n",
                    )
                    addSource(
                        AiSourceType.MEASUREMENT,
                        m.id!!,
                        "Pomiar ${m.type}: ${m.value} ${m.unit}",
                        "Pomiar pobrany precyzyjnym zapytaniem po patient_id.",
                    )
                }
                append("\n")
            }
        }

        if (Category.APPOINTMENTS in categories) {
            val appointments = appointmentRepository.findAllByPatientIdOrderByScheduledAtDesc(patientId, PageRequest.of(0, limit))
            if (appointments.isNotEmpty()) {
                val doctors = doctorRepository.findAllByPatientId(patientId, PageRequest.of(0, 50))
                    .content.associateBy { it.id }
                append("WIZYTY:\n")
                appointments.forEach { appointment ->
                    val doctor = appointment.doctorId?.let { doctors[it] }
                    val doctorLabel = doctor?.let { " | lekarz: dr ${it.firstName} ${it.lastName}" } ?: ""
                    append(
                        "- ${formatInstant(appointment.scheduledAt)}$doctorLabel | status: ${appointment.status}" +
                            " | powód: ${appointment.reason}" +
                            (appointment.recommendations?.let { " | zalecenia: $it" } ?: "") + "\n",
                    )
                    addSource(
                        AiSourceType.APPOINTMENT,
                        appointment.id!!,
                        "Wizyta: ${appointment.reason} (${formatInstant(appointment.scheduledAt)})",
                        "Wizyta pacjenta użyta w kontekście.",
                    )
                }
                append("\n")
            }
        }

        if (Category.CONDITIONS in categories) {
            val conditions = conditionRepository.findAllByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(0, limit))
            if (conditions.isNotEmpty()) {
                append("SCHORZENIA:\n")
                conditions.forEach { condition ->
                    append(
                        "- ${condition.name} | status: ${condition.status} | pochodzenie: ${condition.origin}" +
                            (condition.onsetDate?.let { " | od: $it" } ?: "") + "\n",
                    )
                    addSource(
                        AiSourceType.CONDITION,
                        condition.id!!,
                        "Schorzenie: ${condition.name}",
                        "Schorzenie pacjenta użyte w kontekście.",
                    )
                }
                append("\n")
            }
        }

        if (Category.DOCUMENTS in categories) {
            val documents = documentRepository.findAllByPatientIdOrderByUploadedAtDesc(patientId, PageRequest.of(0, limit)).content
            if (documents.isNotEmpty()) {
                append("DOKUMENTY:\n")
                documents.forEach { document ->
                    append(
                        "- ${document.title} | typ: ${document.documentType} | plik: ${document.originalFilename}" +
                            (document.documentDate?.let { " | data dokumentu: $it" } ?: "") +
                            " | dodano: ${formatInstant(document.uploadedAt)}\n",
                    )
                    addSource(
                        AiSourceType.DOCUMENT,
                        document.id!!,
                        "Dokument: ${document.title}",
                        "Metadane dokumentu; treść nie została jeszcze wyekstrahowana (OCR w ETAPIE 5).",
                    )
                }
                append("\n")
            }
        }

        if (Category.IMAGES in categories) {
            val images = imageRepository.findAllByPatientIdOrderByImageNumberAsc(patientId, PageRequest.of(0, limit))
            if (images.isNotEmpty()) {
                append("ZDJĘCIA:\n")
                images.forEach { image ->
                    append(
                        "- zdjęcie #${image.imageNumber} (UUID ${image.id}) | kategoria: ${image.category}" +
                            (image.title?.let { " | tytuł: $it" } ?: "") +
                            (image.capturedAt?.let { " | wykonano: ${formatInstant(it)}" } ?: "") +
                            " | zapisano: ${formatInstant(image.uploadedAt)}" +
                            (image.symptomId?.let { " | objaw: $it" } ?: "") + "\n",
                    )
                    addSource(
                        AiSourceType.IMAGE,
                        image.id!!,
                        "Zdjęcie #${image.imageNumber}${image.title?.let { " — $it" } ?: ""}",
                        "Zdjęcie pacjenta; plik pozostaje w lokalnym magazynie i nie jest publiczny.",
                    )
                }
                append("\n")
            }
        }

        if (sources.isEmpty()) {
            append("Brak zapisanych danych pasujących do pytania.\n")
        }
        if (truncated) {
            append("\n[Kontekst został obcięty do limitu ${properties.maxContextChars} znaków.]\n")
        }
        return PatientContext(builder.toString(), sources.distinctBy { it.type to it.id }, truncated)
    }

    private fun detectCategories(question: String): Set<Category> {
        val categories = mutableSetOf<Category>()
        fun matches(vararg words: String) = words.any { question.contains(it) }

        if (matches("alerg", "uczul")) categories += Category.ALLERGIES
        if (matches("lek", "medyk", "medic", "dawk", "dose", "tablet", "przyjmowan", "suplement")) categories += Category.MEDICATIONS
        if (matches("objaw", "ból", "bol ", "bólu", "pain", "skór", "skor", "ran", "swęd", "swed", "kaszl", "gorączk", "goraczk")) {
            categories += Category.SYMPTOMS
        }
        if (matches("pomiar", "ciśn", "cisn", "blood", "temperatur", "glukoz", "wag", "heart", "tętno", "tetno", "saturac", "oxygen")) {
            categories += Category.MEASUREMENTS
        }
        if (matches("wizyt", "lekarz", "doctor", "appointment", "konsult", "specjalist")) {
            categories += Category.APPOINTMENTS
        }
        if (matches("chorob", "diagnoz", "condition", "schorz", "rozpoznan")) categories += Category.CONDITIONS
        if (matches("dokument", "pdf", "wynik", "badani", "document", "result", "skan")) categories += Category.DOCUMENTS
        if (matches("zdjęc", "zdjec", "image", "photo", "obraz", "porówn", "porown", "fotograf")) categories += Category.IMAGES

        if (categories.isEmpty()) {
            categories += Category.ALLERGIES
            categories += Category.MEDICATIONS
            categories += Category.SYMPTOMS
            categories += Category.MEASUREMENTS
            categories += Category.APPOINTMENTS
            categories += Category.DOCUMENTS
        }
        if (Category.SYMPTOMS in categories && matches("zdjęc", "zdjec", "image", "photo", "obraz", "fotograf")) {
            categories += Category.IMAGES
        }
        return categories
    }

    private fun profileLine(profile: PatientProfile): String {
        val name = listOfNotNull(profile.firstName, profile.lastName).joinToString(" ").ifBlank { "(imię i nazwisko nieuzupełnione)" }
        val age = profile.dateOfBirth?.let { "${Period.between(it, LocalDate.now()).years} lat" } ?: "wiek nieznany"
        return "Pacjent: $name | $age | płeć: ${profile.sex ?: "brak"} | grupa krwi: ${profile.bloodType ?: "brak"}\n"
    }

    private fun formatInstant(instant: Instant): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(instant.atZone(ZoneOffset.UTC))
}
