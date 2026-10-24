package com.example.ar_project_1.data.resource

import com.google.gson.JsonObject
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordRequestBuilderTest {

    @Test
    fun `create omits empty optional fields`() {
        val json = RecordRequestBuilder.build(
            type = ResourceType.ALLERGIES,
            values = mapOf(
                "substance" to "Penicylina",
                "severity" to "SEVERE",
                "status" to "ACTIVE",
            ),
            isUpdate = false,
        )

        assertEquals("Penicylina", json.get("substance").asString)
        assertEquals("SEVERE", json.get("severity").asString)
        assertFalse(json.has("reaction"))
        assertFalse(json.has("notes"))
    }

    @Test
    fun `update sends null for cleared optional fields`() {
        val json = RecordRequestBuilder.build(
            type = ResourceType.ALLERGIES,
            values = mapOf(
                "substance" to "Penicylina",
                "reaction" to "",
            ),
            isUpdate = true,
        )

        assertTrue(json.get("reaction").isJsonNull)
        assertTrue(json.get("diagnosedAt").isJsonNull)
    }

    @Test
    fun `missing required field is rejected`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            RecordRequestBuilder.build(
                type = ResourceType.ALLERGIES,
                values = mapOf("substance" to "   "),
                isUpdate = false,
            )
        }
        assertTrue(error.message!!.contains("Substancja"))
    }

    @Test
    fun `decimal accepts comma separator`() {
        val json = RecordRequestBuilder.build(
            type = ResourceType.MEASUREMENTS,
            values = mapOf(
                "type" to "BLOOD_PRESSURE",
                "value" to "120,5",
                "unit" to "mmHg",
                "measuredAt" to "2026-01-10 08:30",
            ),
            isUpdate = false,
        )

        assertEquals(BigDecimal("120.5"), json.get("value").asBigDecimal)
        assertTrue(json.get("measuredAt").asString.endsWith("Z"))
    }

    @Test
    fun `invalid date is rejected before request`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            RecordRequestBuilder.build(
                type = ResourceType.ALLERGIES,
                values = mapOf(
                    "substance" to "Penicylina",
                    "diagnosedAt" to "10.01.2026",
                ),
                isUpdate = false,
            )
        }
        assertTrue(error.message!!.contains("RRRR-MM-DD"))
    }

    @Test
    fun `initial values use catalog defaults`() {
        val values = RecordRequestBuilder.initialValues(ResourceType.ALLERGIES)

        assertEquals("UNKNOWN", values["severity"])
        assertEquals("ACTIVE", values["status"])
    }

    @Test
    fun `prefill converts timestamps to form format`() {
        val json = JsonObject().apply {
            addProperty("title", "Ból głowy")
            addProperty("observedAt", "2026-01-10T07:30:00Z")
        }

        val values = RecordRequestBuilder.prefill(ResourceType.SYMPTOMS, json)

        assertEquals("Ból głowy", values["title"])
        assertEquals(DateInputs.toFormValue("2026-01-10T07:30:00Z"), values["observedAt"])
    }

    @Test
    fun `required validation reports first missing field`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            RecordRequestBuilder.validateRequired(
                ResourceType.MEDICATIONS,
                mapOf("name" to ""),
            )
        }
        assertTrue(error.message!!.contains("Nazwa leku"))
    }
}
