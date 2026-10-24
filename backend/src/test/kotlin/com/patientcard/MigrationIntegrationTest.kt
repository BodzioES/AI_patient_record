package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MigrationIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `flyway migration creates the expected schema`() {
        val applied = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true",
            Long::class.java,
        )
        assertTrue((applied ?: 0) >= 1)

        val tables = jdbcTemplate.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String::class.java,
        )
        val expected = setOf(
            "users",
            "patient_profiles",
            "patient_image_counters",
            "allergies",
            "medical_conditions",
            "medications",
            "medication_intake_logs",
            "doctors",
            "appointments",
            "symptoms",
            "measurements",
            "medical_documents",
            "medical_images",
            "image_ai_analyses",
            "medical_document_extractions",
            "chat_conversations",
            "chat_messages",
            "ai_message_sources",
            "ai_processing_jobs",
            "refresh_tokens",
        )
        assertTrue(tables.containsAll(expected), "Missing tables: ${expected - tables.toSet()}")
    }

    @Test
    fun `image numbering constraints exist`() {
        val constraint = jdbcTemplate.queryForObject(
            """
            select count(*) from information_schema.table_constraints
            where table_name = 'medical_images' and constraint_name = 'uk_images_patient_number'
            """.trimIndent(),
            Long::class.java,
        )
        assertEquals(1L, constraint)
    }
}
