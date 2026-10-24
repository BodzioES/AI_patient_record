package com.patientcard

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabaseAndStorage() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE")
        if (Files.exists(storageDirectory)) {
            Files.walk(storageDirectory).use { stream ->
                stream.sorted(Comparator.reverseOrder())
                    .filter { it != storageDirectory }
                    .forEach { Files.deleteIfExists(it) }
            }
        }
    }

    protected data class TestSession(
        val userId: UUID,
        val patientId: UUID,
        val email: String,
        val accessToken: String,
        val refreshToken: String,
    )

    protected fun uniqueEmail(prefix: String = "patient"): String = "$prefix-${UUID.randomUUID()}@example.test"

    protected fun registerUser(
        email: String = uniqueEmail(),
        password: String = TEST_PASSWORD,
        displayName: String = "Test Patient",
    ): TestSession {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerBody(email, password, displayName))),
        ).andReturn()
        check(result.response.status == 201) { "Registration failed: ${result.response.contentAsString}" }
        return toSession(result, email)
    }

    protected fun login(email: String, password: String = TEST_PASSWORD): MvcResult =
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/app/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))),
        ).andReturn()

    protected fun registerBody(email: String, password: String, displayName: String): Map<String, String> =
        mapOf("email" to email, "password" to password, "displayName" to displayName)

    protected fun toSession(result: MvcResult, email: String): TestSession {
        val json = objectMapper.readTree(result.response.contentAsString)
        return TestSession(
            userId = UUID.fromString(json["user"]["id"].asText()),
            patientId = UUID.fromString(json["patientId"].asText()),
            email = email,
            accessToken = json["accessToken"].asText(),
            refreshToken = json["refreshToken"].asText(),
        )
    }

    protected fun authorized(builder: MockHttpServletRequestBuilder, session: TestSession): MockHttpServletRequestBuilder =
        builder.header("Authorization", "Bearer ${session.accessToken}")

    protected fun json(result: MvcResult): JsonNode = objectMapper.readTree(result.response.contentAsString)

    protected fun postJson(path: String, body: Any, session: TestSession): MvcResult =
        mockMvc.perform(
            authorized(
                MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
                session,
            ),
        ).andReturn()

    protected fun putJson(path: String, body: Any, session: TestSession): MvcResult =
        mockMvc.perform(
            authorized(
                MockMvcRequestBuilders.put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
                session,
            ),
        ).andReturn()

    protected fun get(path: String, session: TestSession): MvcResult =
        mockMvc.perform(authorized(MockMvcRequestBuilders.get(path), session)).andReturn()

    protected fun delete(path: String, session: TestSession): MvcResult =
        mockMvc.perform(authorized(MockMvcRequestBuilders.delete(path), session)).andReturn()

    protected fun storedFilePath(storageKey: String): Path = storageDirectory.resolve(storageKey)

    protected fun storedFileCount(): Long =
        if (!Files.exists(storageDirectory)) 0 else Files.walk(storageDirectory).use { it.filter(Files::isRegularFile).count() }

    companion object {
        const val TEST_PASSWORD = "TestPassword123!"

        private val storageDirectory: Path = Files.createTempDirectory("patient-card-test-storage")

        private val postgres: EmbeddedPostgres by lazy {
            EmbeddedPostgres.builder().setPort(0).start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            postgres // force start before properties are registered
            registry.add("spring.datasource.url") { postgres.getJdbcUrl("postgres", "postgres") }
            registry.add("spring.datasource.username") { "postgres" }
            registry.add("spring.datasource.password") { "postgres" }
            registry.add("app.storage.root") { storageDirectory.toString() }
            registry.add("app.jwt.secret") { "test-only-jwt-secret-that-is-long-enough-123456" }
            registry.add("app.ai.provider") { "mock" }
        }
    }
}
