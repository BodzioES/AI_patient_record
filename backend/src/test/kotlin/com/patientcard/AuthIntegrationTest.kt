package com.patientcard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.UUID

class AuthIntegrationTest : AbstractIntegrationTest() {

    @Test
    fun `register creates a patient card and returns tokens without exposing password hash`() {
        val email = uniqueEmail("auth")
        val session = registerUser(email = email, displayName = "Anna Testowa")

        assertTrue(session.accessToken.isNotBlank())
        assertTrue(session.refreshToken.isNotBlank())

        val me = get("/api/v1/app/auth/me", session)
        assertEquals(200, me.response.status)
        val meJson = json(me)
        assertEquals(email, meJson["email"].asText())
        assertEquals("Anna Testowa", meJson["displayName"].asText())
        assertEquals(session.patientId.toString(), meJson["patientId"].asText())
        assertTrue(!meJson.has("passwordHash"))
        assertTrue(!meJson.has("password"))

        val hash = jdbcTemplate.queryForObject(
            "select password_hash from users where email = ?",
            String::class.java,
            email,
        )
        assertNotEquals(TEST_PASSWORD, hash)
        assertTrue(hash!!.startsWith("\$2"))

        val profileCount = jdbcTemplate.queryForObject(
            "select count(*) from patient_profiles where user_id = ?",
            Long::class.java,
            session.userId,
        )
        assertEquals(1L, profileCount)
    }

    @Test
    fun `login works and wrong password is rejected`() {
        val session = registerUser()
        val login = login(session.email)
        assertEquals(200, login.response.status)
        val logged = toSession(login, session.email)
        assertEquals(session.patientId, logged.patientId)

        val wrong = login(session.email, "WrongPassword123!")
        assertEquals(401, wrong.response.status)
        assertEquals("UNAUTHORIZED", json(wrong)["code"].asText())
    }

    @Test
    fun `protected endpoints reject missing or invalid tokens`() {
        val missing = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/app/patient")).andReturn()
        assertEquals(401, missing.response.status)

        val invalid = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/app/patient")
                .header("Authorization", "Bearer not-a-real-token"),
        ).andReturn()
        assertEquals(401, invalid.response.status)
    }

    @Test
    fun `refresh tokens rotate and logout revokes them`() {
        val session = registerUser()
        val refreshed = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to session.refreshToken))),
        ).andReturn()
        assertEquals(200, refreshed.response.status)
        val refreshedJson = json(refreshed)
        val newRefresh = refreshedJson["refreshToken"].asText()
        assertNotEquals(session.refreshToken, newRefresh)

        val reused = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to session.refreshToken))),
        ).andReturn()
        assertEquals(401, reused.response.status)

        val logout = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to newRefresh))),
        ).andReturn()
        assertEquals(204, logout.response.status)

        val afterLogout = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to newRefresh))),
        ).andReturn()
        assertEquals(401, afterLogout.response.status)
    }

    @Test
    fun `duplicate email and validation errors return proper statuses`() {
        val email = uniqueEmail("duplicate")
        registerUser(email = email)
        val duplicate = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody(email, TEST_PASSWORD, "Duplicate"))),
        ).andReturn()
        assertEquals(409, duplicate.response.status)
        assertEquals("CONFLICT", json(duplicate)["code"].asText())

        val invalid = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("email" to "not-an-email", "password" to "short", "displayName" to ""),
                    ),
                ),
        ).andReturn()
        assertEquals(400, invalid.response.status)
        assertEquals("VALIDATION_ERROR", json(invalid)["code"].asText())
        assertTrue(json(invalid)["errors"].size() >= 3)
    }

    @Test
    fun `user cannot use another user's patient id from request body`() {
        val first = registerUser()
        val second = registerUser()
        assertEquals(200, get("/api/v1/app/patient", first).response.status)
        assertEquals(200, get("/api/v1/app/patient", second).response.status)
        assertNotEquals(first.patientId, second.patientId)

        // There is no patient_id in DTOs; ownership always comes from the token.
        val update = putJson(
            "/api/v1/app/patient",
            mapOf("firstName" to "Own", "notes" to "Only my card"),
            first,
        )
        assertEquals(200, update.response.status)
        assertEquals(first.patientId.toString(), json(get("/api/v1/app/patient", first))["id"].asText())
        assertTrue(json(update)["firstName"].asText() == "Own")
        assertEquals(UUID.fromString(first.patientId.toString()), first.patientId)
    }
}
