package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.Base64
import java.util.UUID

class AiSkeletonIntegrationTest : AbstractIntegrationTest() {

    private val pngBytes: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
    )

    private fun seedClinicalData(session: TestSession) {
        val medication = postJson(
            "/api/v1/app/medications",
            mapOf("name" to "Ramipryl", "strength" to "5 mg", "status" to "ACTIVE"),
            session,
        )
        assertEquals(201, medication.response.status)
        val symptom = postJson(
            "/api/v1/app/symptoms",
            mapOf("title" to "Kaszel", "severity" to 4, "observedAt" to "2026-01-12T09:00:00Z", "status" to "ACTIVE"),
            session,
        )
        assertEquals(201, symptom.response.status)
    }

    private fun uploadImage(session: TestSession): UUID {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/app/images")
                .file(MockMultipartFile("file", "skin.png", "image/png", pngBytes))
                .param("title", "Zmiana na skórze")
                .param("category", "SKIN")
                .header("Authorization", "Bearer ${session.accessToken}"),
        ).andReturn()
        assertEquals(201, result.response.status)
        return UUID.fromString(json(result)["id"].asText())
    }

    @Test
    fun `chat uses mock provider, saves history and validates sources`() {
        val owner = registerUser(uniqueEmail("ai"))
        val stranger = registerUser(uniqueEmail("ai-stranger"))
        seedClinicalData(owner)

        val response = postJson(
            "/api/v1/ai/chat",
            mapOf("message" to "Jakie przyjmuję leki i jakie mam objawy?"),
            owner,
        )
        assertEquals(200, response.response.status)
        val body = json(response)
        assertTrue(body["mock"].asBoolean())
        assertTrue(body["answer"].asText().contains("MOCK"))
        assertEquals("mock-ai-provider", body["modelName"].asText())
        val conversationId = body["conversationId"].asText()
        assertFalse(body["sources"].isEmpty)
        val types = body["sources"].map { it["type"].asText() }.toSet()
        assertTrue(types.contains("MEDICATION"), "Sources: $types")
        assertTrue(types.contains("SYMPTOM"), "Sources: $types")

        val conversations = get("/api/v1/ai/conversations", owner)
        assertEquals(200, conversations.response.status)
        assertEquals(1, json(conversations)["totalElements"].asInt())
        assertEquals(2, json(conversations)["content"][0]["messageCount"].asInt())

        val detail = get("/api/v1/ai/conversations/$conversationId", owner)
        assertEquals(200, detail.response.status)
        assertEquals(2, json(detail)["messages"].size())
        assertEquals("USER", json(detail)["messages"][0]["role"].asText())
        assertEquals("ASSISTANT", json(detail)["messages"][1]["role"].asText())

        // Isolation of conversations.
        assertEquals(404, get("/api/v1/ai/conversations/$conversationId", stranger).response.status)
        assertEquals(404, delete("/api/v1/ai/conversations/$conversationId", stranger).response.status)
        val crossChat = postJson(
            "/api/v1/ai/chat",
            mapOf("conversationId" to conversationId, "message" to "Kontynuacja"),
            stranger,
        )
        assertEquals(404, crossChat.response.status)

        val followUp = postJson(
            "/api/v1/ai/chat",
            mapOf("conversationId" to conversationId, "message" to "A jakie mam pomiary?"),
            owner,
        )
        assertEquals(200, followUp.response.status)
        assertEquals(conversationId, json(followUp)["conversationId"].asText())

        assertEquals(204, delete("/api/v1/ai/conversations/$conversationId", owner).response.status)
        assertEquals(404, get("/api/v1/ai/conversations/$conversationId", owner).response.status)
    }

    @Test
    fun `image analysis is clearly marked as mock and stored as a separate record`() {
        val owner = registerUser(uniqueEmail("ai-image"))
        val stranger = registerUser(uniqueEmail("ai-image-stranger"))
        seedClinicalData(owner)
        val imageId = uploadImage(owner)

        val analysis = postJson("/api/v1/ai/images/$imageId/analyze", emptyMap<String, String>(), owner)
        assertEquals(200, analysis.response.status)
        val body = json(analysis)
        assertTrue(body["mock"].asBoolean())
        assertTrue(body["analysisText"].asText().contains("MOCK"))
        assertEquals("mock-image-v1", body["promptVersion"].asText())
        assertEquals(1, body["imageNumber"].asLong())
        assertTrue(body["notice"].asText().contains("MockAiProvider"))

        val stored = get("/api/v1/ai/images/$imageId/analysis", owner)
        assertEquals(200, stored.response.status)
        assertEquals(1, json(stored)["analyses"].size())

        val image = get("/api/v1/app/images/$imageId", owner)
        assertEquals("COMPLETED", json(image)["aiProcessingStatus"].asText())

        assertEquals(404, postJson("/api/v1/ai/images/$imageId/analyze", emptyMap<String, String>(), stranger).response.status)
        assertEquals(404, get("/api/v1/ai/images/$imageId/analysis", stranger).response.status)
    }
}
