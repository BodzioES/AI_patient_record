package com.example.ar_project_1.data.resource

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateInputs {

    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun toInstantString(input: String): String {
        val value = input.trim().replace(' ', 'T')
        if (value.isEmpty()) throw IllegalArgumentException("Data jest wymagana.")
        return try {
            when {
                DATE_PATTERN.matches(value) -> LocalDate.parse(value, DATE_FORMAT)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toString()

                value.endsWith("Z") || OFFSET_PATTERN.containsMatchIn(value) ->
                    OffsetDateTime.parse(value).toInstant().toString()

                else -> LocalDateTime.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toString()
            }
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Podaj datę i czas, np. 2026-01-10 08:30.")
        }
    }

    fun toDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DISPLAY_FORMAT)
        } catch (_: Exception) {
            iso
        }
    }

    fun toFormValue(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DISPLAY_FORMAT)
        } catch (_: Exception) {
            iso
        }
    }

    private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val OFFSET_PATTERN = Regex("[+-]\\d{2}:\\d{2}$")
}
