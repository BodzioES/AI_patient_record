package com.example.ar_project_1.data.api

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorParserTest {

    @Test
    fun `reads problem detail code and field errors`() {
        val body = """
            {
              "type": "about:blank",
              "title": "Bad Request",
              "status": 400,
              "detail": "Adres email jest nieprawidłowy.",
              "code": "VALIDATION_ERROR",
              "errors": { "email": "Podaj poprawny email." }
            }
        """.trimIndent()

        val error = ApiErrorParser.parse(400, body)

        assertEquals("Adres email jest nieprawidłowy.", error.message)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(400, error.status)
        assertEquals("Podaj poprawny email.", error.fieldErrors["email"])
    }

    @Test
    fun `falls back to status message when body is empty`() {
        val error = ApiErrorParser.parse(500, null)

        assertEquals("Wystąpił błąd serwera (500).", error.message)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun `falls back when body is not json`() {
        val error = ApiErrorParser.parse(400, "plain text")

        assertEquals("Dane są nieprawidłowe.", error.message)
    }

    @Test
    fun `maps 401 to session message`() {
        assertEquals(
            "Sesja wygasła. Zaloguj się ponownie.",
            ApiErrorParser.parse(401, "").message,
        )
    }

    @Test
    fun `network error has stable code`() {
        val error = ApiErrorParser.network(IOException("timeout"))

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.message.contains("Brak połączenia"))
    }
}
