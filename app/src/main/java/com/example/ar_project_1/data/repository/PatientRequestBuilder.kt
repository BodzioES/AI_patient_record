package com.example.ar_project_1.data.repository

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.time.LocalDate

/** Builds the PUT /app/patient body; blank fields are sent as explicit nulls. */
internal object PatientRequestBuilder {

    private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun build(values: Map<String, String>): JsonObject {
        val json = JsonObject()
        fun text(key: String) {
            val value = values[key]?.trim().orEmpty()
            json.add(key, if (value.isEmpty()) JsonNull.INSTANCE else JsonPrimitive(value))
        }
        text("firstName")
        text("lastName")
        text("notes")

        val date = values["dateOfBirth"]?.trim().orEmpty()
        if (date.isEmpty()) {
            json.add("dateOfBirth", JsonNull.INSTANCE)
        } else {
            if (!DATE_PATTERN.matches(date)) {
                throw IllegalArgumentException("Data urodzenia musi mieć format RRRR-MM-DD.")
            }
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                ?: throw IllegalArgumentException("Data urodzenia jest nieprawidłowa.")
            if (parsed.isAfter(LocalDate.now())) {
                throw IllegalArgumentException("Data urodzenia nie może być w przyszłości.")
            }
            json.add("dateOfBirth", JsonPrimitive(date))
        }

        fun enumValue(key: String) {
            val value = values[key]?.trim().orEmpty()
            json.add(key, if (value.isEmpty()) JsonNull.INSTANCE else JsonPrimitive(value))
        }
        enumValue("sex")
        enumValue("bloodType")

        fun decimal(key: String) {
            val value = values[key]?.trim().orEmpty().replace(',', '.')
            if (value.isEmpty()) {
                json.add(key, JsonNull.INSTANCE)
            } else {
                val number = value.toBigDecimalOrNull()
                    ?: throw IllegalArgumentException("Pole musi być liczbą.")
                json.add(key, JsonPrimitive(number))
            }
        }
        decimal("heightCm")
        decimal("weightKg")
        return json
    }
}
