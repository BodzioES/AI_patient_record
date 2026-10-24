package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.nio.file.Files
import java.util.Base64
import java.util.UUID

class FileIntegrationTest : AbstractIntegrationTest() {

    private val pngBytes: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
    )

    private fun createSymptom(session: TestSession): String {
        val result = postJson(
            "/api/v1/app/symptoms",
            mapOf("title" to "Zmiana skórna", "observedAt" to "2026-01-05T10:00:00Z", "status" to "ACTIVE"),
            session,
        )
        assertEquals(201, result.response.status)
        return json(result)["id"].asText()
    }

    private fun uploadImage(
        session: TestSession,
        filename: String = "photo.png",
        contentType: String = "image/png",
        bytes: ByteArray = pngBytes,
        symptomId: String? = null,
        title: String = "Zdjęcie testowe",
    ): MvcResult {
        val request = MockMvcRequestBuilders.multipart("/api/v1/app/images")
            .file(MockMultipartFile("file", filename, contentType, bytes))
            .param("title", title)
            .param("category", "SKIN")
            .header("Authorization", "Bearer ${session.accessToken}")
        if (symptomId != null) request.param("symptomId", symptomId)
        return mockMvc.perform(request).andReturn()
    }

    private fun uploadDocument(
        session: TestSession,
        filename: String = "wynik.pdf",
        contentType: String = "application/pdf",
        bytes: ByteArray = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".toByteArray(),
    ): MvcResult = mockMvc.perform(
        MockMvcRequestBuilders.multipart("/api/v1/app/documents")
            .file(MockMultipartFile("file", filename, contentType, bytes))
            .param("documentType", "LAB_RESULT")
            .param("title", "Wynik badania")
            .param("documentDate", "2026-01-08")
            .header("Authorization", "Bearer ${session.accessToken}"),
    ).andReturn()

    @Test
    fun `images receive stable per-patient numbers and can be linked to a symptom`() {
        val owner = registerUser(uniqueEmail("images"))
        val stranger = registerUser(uniqueEmail("images-stranger"))
        val symptomId = createSymptom(owner)

        val first = uploadImage(owner, symptomId = symptomId)
        assertEquals(201, first.response.status)
        val firstJson = json(first)
        val firstId = firstJson["id"].asText()
        assertEquals(1, firstJson["imageNumber"].asLong())
        assertNotNull(firstJson["uploadedAt"].asText())
        assertEquals(symptomId, firstJson["symptomId"].asText())
        assertEquals(64, firstJson["sha256Checksum"].asText().length)
        assertFalse(firstJson.has("storageKey"))

        val second = uploadImage(owner, title = "Zdjęcie drugie")
        assertEquals(201, second.response.status)
        assertEquals(2, json(second)["imageNumber"].asLong())

        val third = uploadImage(owner, title = "Zdjęcie trzecie")
        assertEquals(201, third.response.status)
        assertEquals(3, json(third)["imageNumber"].asLong())

        val storageKey = jdbcTemplate.queryForObject(
            "select storage_key from medical_images where id = ?",
            String::class.java,
            UUID.fromString(firstId),
        )
        assertNotNull(storageKey)
        assertTrue(Files.exists(storedFilePath(storageKey!!)))

        val secondId = json(second)["id"].asText()
        val secondStorageKey = jdbcTemplate.queryForObject(
            "select storage_key from medical_images where id = ?",
            String::class.java,
            UUID.fromString(secondId),
        )!!
        assertEquals(204, delete("/api/v1/app/images/$secondId", owner).response.status)
        assertFalse(Files.exists(storedFilePath(secondStorageKey)))
        assertTrue(Files.exists(storedFilePath(storageKey)))

        // A deleted number is never reused.
        val fourth = uploadImage(owner, title = "Zdjęcie czwarte")
        assertEquals(201, fourth.response.status)
        assertEquals(4, json(fourth)["imageNumber"].asLong())

        val list = get("/api/v1/app/images?size=20", owner)
        assertEquals(200, list.response.status)
        val numbers = json(list)["content"].map { it["imageNumber"].asLong() }
        assertEquals(listOf(4L, 3L, 1L), numbers)

        val linked = get("/api/v1/app/images?symptomId=$symptomId", owner)
        assertEquals(1, json(linked)["totalElements"].asInt())

        val download = get("/api/v1/app/images/$firstId/file", owner)
        assertEquals(200, download.response.status)
        assertEquals("image/png", download.response.contentType)
        assertTrue(download.response.contentAsByteArray.contentEquals(pngBytes))

        // Isolation.
        assertEquals(404, get("/api/v1/app/images/$firstId", stranger).response.status)
        assertEquals(404, get("/api/v1/app/images/$firstId/file", stranger).response.status)
        assertEquals(404, delete("/api/v1/app/images/$firstId", stranger).response.status)

        val crossLink = uploadImage(stranger, symptomId = symptomId)
        assertEquals(400, crossLink.response.status)

        val invalid = uploadImage(owner, filename = "malware.exe", contentType = "application/octet-stream", bytes = byteArrayOf(0x4D, 0x5A))
        assertEquals(415, invalid.response.status)
    }

    @Test
    fun `documents upload download and delete with owner checks`() {
        val owner = registerUser(uniqueEmail("docs"))
        val stranger = registerUser(uniqueEmail("docs-stranger"))

        val upload = uploadDocument(owner)
        assertEquals(201, upload.response.status)
        val body = json(upload)
        val documentId = body["id"].asText()
        assertEquals("LAB_RESULT", body["documentType"].asText())
        assertFalse(body.has("storageKey"))
        assertEquals(64, body["sha256Checksum"].asText().length)
        assertEquals("PENDING", body["processingStatus"].asText())

        val storageKey = jdbcTemplate.queryForObject(
            "select storage_key from medical_documents where id = ?",
            String::class.java,
            UUID.fromString(documentId),
        )
        assertTrue(Files.exists(storedFilePath(storageKey!!)))

        val download = get("/api/v1/app/documents/$documentId/file", owner)
        assertEquals(200, download.response.status)
        assertEquals(MediaType.APPLICATION_PDF_VALUE, download.response.contentType)
        assertTrue(download.response.contentAsString.contains("%PDF"))

        val list = get("/api/v1/app/documents", owner)
        assertEquals(1, json(list)["totalElements"].asInt())

        assertEquals(404, get("/api/v1/app/documents/$documentId", stranger).response.status)
        assertEquals(404, get("/api/v1/app/documents/$documentId/file", stranger).response.status)

        val invalidDocument = uploadDocument(owner, filename = "fake.pdf", contentType = "application/pdf", bytes = "not a pdf".toByteArray())
        assertEquals(415, invalidDocument.response.status)

        assertEquals(204, delete("/api/v1/app/documents/$documentId", owner).response.status)
        assertFalse(Files.exists(storedFilePath(storageKey)))
        assertEquals(404, get("/api/v1/app/documents/$documentId", owner).response.status)
    }

    @Test
    fun `storage is not exposed as public static content`() {
        val session = registerUser()
        val unauthenticated = mockMvc.perform(MockMvcRequestBuilders.get("/storage/patients/x/y.png")).andReturn()
        assertEquals(401, unauthenticated.response.status)

        val authenticated = get("/storage/patients/x/y.png", session)
        assertEquals(404, authenticated.response.status)
    }
}
