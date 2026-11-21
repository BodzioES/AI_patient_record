package com.patientcard.ai.context

import com.patientcard.config.AiProperties
import com.patientcard.entity.Allergy
import com.patientcard.entity.AllergySeverity
import com.patientcard.entity.Measurement
import com.patientcard.entity.MeasurementType
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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class DefaultPatientContextProviderTest {

    private val patientId = UUID.randomUUID()
    private val profiles = mockk<PatientProfileRepository>()
    private val allergies = mockk<AllergyRepository>()
    private val medications = mockk<MedicationRepository>()
    private val symptoms = mockk<SymptomRepository>()
    private val measurements = mockk<MeasurementRepository>()
    private val appointments = mockk<AppointmentRepository>()
    private val doctors = mockk<DoctorRepository>()
    private val conditions = mockk<MedicalConditionRepository>()
    private val documents = mockk<MedicalDocumentRepository>()
    private val images = mockk<MedicalImageRepository>()

    private val provider = DefaultPatientContextProvider(
        profiles, allergies, medications, symptoms, measurements, appointments,
        doctors, conditions, documents, images, AiProperties(maxContextItemsPerCategory = 3),
    )

    init {
        every { profiles.findById(patientId) } returns Optional.empty()
        every { allergies.findAllByPatientIdOrderByCreatedAtDesc(patientId, any()) } returns listOf(
            Allergy(patientId = patientId, substance = "Ibuprofen", severity = AllergySeverity.MODERATE),
        )
        every { medications.findAllByPatientIdOrderByCreatedAtDesc(patientId, any()) } returns emptyList()
        every { symptoms.findAllByPatientIdOrderByObservedAtDesc(patientId, any()) } returns emptyList()
        every { appointments.findAllByPatientIdOrderByScheduledAtDesc(patientId, any()) } returns emptyList()
        every { conditions.findAllByPatientIdOrderByCreatedAtDesc(patientId, any()) } returns emptyList()
        every { documents.findAllByPatientIdOrderByUploadedAtDesc(patientId, any()) } returns Page.empty()
        every { images.findAllByPatientIdOrderByImageNumberAsc(patientId, any()) } returns emptyList()
        every { measurements.findAllByPatientIdAndType(patientId, any(), any()) } answers {
            val type = secondArg<MeasurementType>()
            val pageable = thirdArg<Pageable>()
            val items = when (type) {
                MeasurementType.BLOOD_PRESSURE -> (1..12).map { measurement(type, 120 + it, "mmHg", it) }
                MeasurementType.GLUCOSE -> listOf(measurement(type, 135, "mg/dl", 30))
                else -> emptyList()
            }
            PageImpl(items.take(pageable.pageSize))
        }
    }

    @Test
    fun `question about a drug by name always includes allergies`() {
        val context = provider.buildContext(patientId, "Czy mogę wziąć ibuprofen na ból głowy?")

        assertTrue(context.text.contains("ALERGIE:"), context.text)
        assertTrue(context.text.contains("Ibuprofen"), context.text)
    }

    @Test
    fun `measurement limit applies per type so one series does not push others out`() {
        val context = provider.buildContext(patientId, "Jakie mam pomiary ciśnienia i glukozy?")

        assertTrue(context.text.contains("GLUCOSE: 135"), context.text)
        assertTrue(context.text.contains("BLOOD_PRESSURE: 121"), context.text)
        assertFalse(context.text.contains("BLOOD_PRESSURE: 124"), "limit 3 per type: ${context.text}")
    }

    private fun measurement(type: MeasurementType, value: Int, unit: String, daysAgo: Int) = Measurement(
        patientId = patientId,
        type = type,
        value = BigDecimal(value),
        unit = unit,
        measuredAt = Instant.now().minusSeconds(daysAgo * 86_400L),
    ).apply { id = UUID.randomUUID() }
}
