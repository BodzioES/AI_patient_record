package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ClinicalDataIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `full clinical data flow with isolation between patients`() {
        val owner = registerUser(uniqueEmail("owner"))
        val stranger = registerUser(uniqueEmail("stranger"))

        val patientUpdate = putJson(
            "/api/v1/app/patient",
            mapOf(
                "firstName" to "Jan",
                "lastName" to "Kowalski",
                "dateOfBirth" to "1985-04-12",
                "sex" to "MALE",
                "heightCm" to 180.5,
                "weightKg" to 82.0,
                "bloodType" to "A_POSITIVE",
                "notes" to "Fikcyjny pacjent testowy",
            ),
            owner,
        )
        assertEquals(200, patientUpdate.response.status)
        assertEquals("Jan", json(patientUpdate)["firstName"].asText())

        val doctor = postJson(
            "/api/v1/app/doctors",
            mapOf(
                "firstName" to "Ewa",
                "lastName" to "Lekarska",
                "specialty" to "Kardiologia",
                "facilityName" to "Przychodnia Testowa",
                "phone" to "500-000-000",
                "email" to "ewa.lekarska@example.test",
            ),
            owner,
        )
        assertEquals(201, doctor.response.status)
        val doctorId = json(doctor)["id"].asText()

        val medication = postJson(
            "/api/v1/app/medications",
            mapOf(
                "name" to "Metformina",
                "activeIngredient" to "metformini hydrochloridum",
                "strength" to "500 mg",
                "dosageForm" to "tabletka",
                "prescribedDose" to "1 tabletka",
                "route" to "doustnie",
                "frequency" to "2 razy dziennie",
                "instructions" to "Przyjmować po posiłku",
                "startDate" to "2026-01-01",
                "prescribingDoctorId" to doctorId,
                "status" to "ACTIVE",
            ),
            owner,
        )
        assertEquals(201, medication.response.status)
        val medicationId = json(medication)["id"].asText()

        val intake = postJson(
            "/api/v1/app/medication-intake",
            mapOf(
                "medicationId" to medicationId,
                "scheduledAt" to "2026-01-10T08:00:00Z",
                "takenAt" to "2026-01-10T08:05:00Z",
                "amount" to 1.0,
                "unit" to "tabletka",
                "status" to "TAKEN",
            ),
            owner,
        )
        assertEquals(201, intake.response.status)
        val intakeId = json(intake)["id"].asText()

        val allergy = postJson(
            "/api/v1/app/allergies",
            mapOf(
                "substance" to "Penicylina",
                "reaction" to "Wysypka",
                "severity" to "MODERATE",
                "status" to "ACTIVE",
                "diagnosedAt" to "2020-06-01",
            ),
            owner,
        )
        assertEquals(201, allergy.response.status)
        val allergyId = json(allergy)["id"].asText()

        val condition = postJson(
            "/api/v1/app/conditions",
            mapOf(
                "name" to "Cukrzyca typu 2",
                "description" to "Rozpoznanie fikcyjne do testów",
                "status" to "ACTIVE",
                "onsetDate" to "2019-02-01",
                "origin" to "CLINICIAN_DOCUMENTED",
            ),
            owner,
        )
        assertEquals(201, condition.response.status)
        val conditionId = json(condition)["id"].asText()

        val symptom = postJson(
            "/api/v1/app/symptoms",
            mapOf(
                "title" to "Ból głowy",
                "description" to "Ucisk w skroniach",
                "bodyLocation" to "głowa",
                "severity" to 6,
                "startedAt" to "2026-01-09T18:00:00Z",
                "observedAt" to "2026-01-09T18:30:00Z",
                "status" to "ACTIVE",
            ),
            owner,
        )
        assertEquals(201, symptom.response.status)
        val symptomId = json(symptom)["id"].asText()

        val appointment = postJson(
            "/api/v1/app/appointments",
            mapOf(
                "doctorId" to doctorId,
                "scheduledAt" to "2026-02-01T09:00:00Z",
                "location" to "Przychodnia Testowa, pokój 3",
                "reason" to "Kontrola",
                "status" to "SCHEDULED",
            ),
            owner,
        )
        assertEquals(201, appointment.response.status)
        val appointmentId = json(appointment)["id"].asText()

        val measurement = postJson(
            "/api/v1/app/measurements",
            mapOf(
                "type" to "BLOOD_PRESSURE",
                "value" to 130.0,
                "secondaryValue" to 85.0,
                "unit" to "mmHg",
                "measuredAt" to "2026-01-10T07:30:00Z",
                "source" to "MANUAL",
            ),
            owner,
        )
        assertEquals(201, measurement.response.status)
        val measurementId = json(measurement)["id"].asText()

        val medications = get("/api/v1/app/medications?size=10", owner)
        assertEquals(200, medications.response.status)
        assertEquals(1, json(medications)["totalElements"].asInt())
        assertEquals("Metformina", json(medications)["content"][0]["name"].asText())

        val intakeList = get("/api/v1/app/medication-intake?medicationId=$medicationId", owner)
        assertEquals(200, intakeList.response.status)
        assertEquals(1, json(intakeList)["totalElements"].asInt())

        val filtered = get("/api/v1/app/allergies?status=ACTIVE", owner)
        assertEquals(200, filtered.response.status)
        assertEquals(1, json(filtered)["totalElements"].asInt())

        val paged = get("/api/v1/app/measurements?size=1&page=0", owner)
        assertEquals(1, json(paged)["content"].size())
        assertEquals(1, json(paged)["totalElements"].asInt())

        // Stranger cannot read or modify any of the owner's records.
        listOf(
            "/api/v1/app/doctors/$doctorId",
            "/api/v1/app/medications/$medicationId",
            "/api/v1/app/medication-intake/$intakeId",
            "/api/v1/app/allergies/$allergyId",
            "/api/v1/app/conditions/$conditionId",
            "/api/v1/app/symptoms/$symptomId",
            "/api/v1/app/appointments/$appointmentId",
            "/api/v1/app/measurements/$measurementId",
        ).forEach { path ->
            assertEquals(404, get(path, stranger).response.status, "Expected 404 for $path")
        }

        assertEquals(
            404,
            putJson(
                "/api/v1/app/medications/$medicationId",
                mapOf("name" to "Hacked", "status" to "ACTIVE"),
                stranger,
            ).response.status,
        )
        assertEquals(404, delete("/api/v1/app/allergies/$allergyId", stranger).response.status)

        val crossDoctor = postJson(
            "/api/v1/app/medications",
            mapOf("name" to "Obcy lek", "status" to "ACTIVE", "prescribingDoctorId" to doctorId),
            stranger,
        )
        assertEquals(400, crossDoctor.response.status)

        val strangerPatient = json(get("/api/v1/app/patient", stranger))
        assertFalse(strangerPatient.has("notes") && strangerPatient["notes"].asText() == "Fikcyjny pacjent testowy")

        // Owner can update and delete.
        val updated = putJson(
            "/api/v1/app/symptoms/$symptomId",
            mapOf(
                "title" to "Ból głowy (ustępuje)",
                "severity" to 3,
                "observedAt" to "2026-01-11T08:00:00Z",
                "status" to "MONITORING",
            ),
            owner,
        )
        assertEquals(200, updated.response.status)
        assertEquals("MONITORING", json(updated)["status"].asText())

        assertEquals(204, delete("/api/v1/app/appointments/$appointmentId", owner).response.status)
        assertEquals(404, get("/api/v1/app/appointments/$appointmentId", owner).response.status)

        // Repeated observation of the same symptom is a new row, not an overwrite.
        val secondObservation = postJson(
            "/api/v1/app/symptoms",
            mapOf(
                "title" to "Ból głowy",
                "severity" to 4,
                "observedAt" to "2026-01-12T08:00:00Z",
                "status" to "ACTIVE",
            ),
            owner,
        )
        assertEquals(201, secondObservation.response.status)
        val allSymptoms = get("/api/v1/app/symptoms?size=10", owner)
        assertEquals(2, json(allSymptoms)["totalElements"].asInt())
    }

    @Test
    fun `invalid dates and enums are rejected`() {
        val session = registerUser()
        val badMedication = postJson(
            "/api/v1/app/medications",
            mapOf(
                "name" to "Lek",
                "startDate" to "2026-02-01",
                "endDate" to "2026-01-01",
                "status" to "ACTIVE",
            ),
            session,
        )
        assertEquals(400, badMedication.response.status)

        val badSeverity = postJson(
            "/api/v1/app/symptoms",
            mapOf("title" to "Objaw", "severity" to 11, "observedAt" to "2026-01-01T10:00:00Z"),
            session,
        )
        assertEquals(400, badSeverity.response.status)

        val badEnum = postJson(
            "/api/v1/app/medications",
            mapOf("name" to "Lek", "status" to "NOT_A_STATUS"),
            session,
        )
        assertEquals(400, badEnum.response.status)
    }
}
