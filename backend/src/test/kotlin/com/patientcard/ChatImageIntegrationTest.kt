package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.Base64

class ChatImageIntegrationTest : AbstractIntegrationTest() {

    private val pngBytes: ByteArray = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
    )

    private fun upload(session: TestSession, filename: String, contentType: String): MvcResult =
        mockMvc.perform(
            MockMvcRequestBuilders.multipart("/api/v1/app/images")
                .file(MockMultipartFile("file", filename, contentType, pngBytes))
                .param("category", "OTHER")
                .header("Authorization", "Bearer ${session.accessToken}"),
        ).andReturn()

    @Test
    fun `android style uploads without extension or with alias mime are accepted`() {
        val session = registerUser(uniqueEmail("android-upload"))

        val noExtension = upload(session, "1000012345", "application/octet-stream")
        assertEquals(201, noExtension.response.status)
        assertEquals("image/png", json(noExtension)["mimeType"].asText())
        assertEquals(1, json(noExtension)["widthPx"].asInt())
        assertEquals(1, json(noExtension)["heightPx"].asInt())

        val aliasMime = upload(session, "photo.png", "image/*")
        assertEquals(201, aliasMime.response.status)

        // Declared format that contradicts the content is still rejected.
        val lying = upload(session, "photo.jpg", "image/jpeg")
        assertEquals(415, lying.response.status)
    }

    @Test
    fun `chat accepts attached images and stores them with the user message`() {
        val owner = registerUser(uniqueEmail("chat-image"))
        val stranger = registerUser(uniqueEmail("chat-image-stranger"))
        val imageId = json(upload(owner, "skin.png", "image/png"))["id"].asText()

        val imageOnly = postJson("/api/v1/ai/chat", mapOf("message" to "", "imageIds" to listOf(imageId)), owner)
        assertEquals(200, imageOnly.response.status)
        val conversationId = json(imageOnly)["conversationId"].asText()
        assertTrue(json(imageOnly)["answer"].asText().contains("#1"))

        val detail = json(get("/api/v1/ai/conversations/$conversationId", owner))
        val userMessage = detail["messages"][0]
        assertEquals("USER", userMessage["role"].asText())
        assertEquals(imageId, userMessage["imageIds"][0].asText())
        assertTrue(userMessage["content"].asText().isNotBlank())

        val empty = postJson("/api/v1/ai/chat", mapOf("message" to "  "), owner)
        assertEquals(400, empty.response.status)

        val foreign = postJson("/api/v1/ai/chat", mapOf("message" to "Co to?", "imageIds" to listOf(imageId)), stranger)
        assertEquals(400, foreign.response.status)

        val tooMany = postJson(
            "/api/v1/ai/chat",
            mapOf("message" to "Dużo", "imageIds" to List(5) { imageId }),
            owner,
        )
        assertEquals(400, tooMany.response.status)
    }
}
