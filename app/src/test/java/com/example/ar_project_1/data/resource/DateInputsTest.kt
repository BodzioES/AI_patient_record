package com.example.ar_project_1.data.resource

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DateInputsTest {

    @Test
    fun `date only is converted to start of day instant`() {
        val expected = LocalDate.parse("2026-01-10")
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toString()

        assertEquals(expected, DateInputs.toInstantString("2026-01-10"))
    }

    @Test
    fun `date and time is converted using local zone`() {
        val expected = LocalDateTime.parse("2026-01-10T08:30")
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toString()

        assertEquals(expected, DateInputs.toInstantString("2026-01-10 08:30"))
    }

    @Test
    fun `iso instant is preserved`() {
        assertEquals(
            "2026-01-10T07:30:00Z",
            DateInputs.toInstantString("2026-01-10T07:30:00Z"),
        )
    }

    @Test
    fun `invalid value throws readable error`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            DateInputs.toInstantString("10.01.2026")
        }
        assertEquals("Podaj datę i czas, np. 2026-01-10 08:30.", error.message)
    }

    @Test
    fun `empty value is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DateInputs.toInstantString("  ")
        }
    }

    @Test
    fun `display and form value use the same local format`() {
        val iso = "2026-01-10T07:30:00Z"
        val expected = java.time.Instant.parse(iso)
            .atZone(ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        assertEquals(expected, DateInputs.toDisplay(iso))
        assertEquals(expected, DateInputs.toFormValue(iso))
    }

    @Test
    fun `unknown value is returned unchanged`() {
        assertEquals("not-a-date", DateInputs.toDisplay("not-a-date"))
        assertEquals("", DateInputs.toDisplay(null))
    }
}
