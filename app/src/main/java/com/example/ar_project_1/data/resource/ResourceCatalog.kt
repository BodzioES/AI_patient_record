package com.example.ar_project_1.data.resource

enum class ResourceType(
    val route: String,
    val endpoint: String,
    val title: String,
    val singular: String,
) {
    ALLERGIES("allergies", "app/allergies", "Alergie", "alergię"),
    CONDITIONS("conditions", "app/conditions", "Schorzenia", "schorzenie"),
    MEDICATIONS("medications", "app/medications", "Leki", "lek"),
    SYMPTOMS("symptoms", "app/symptoms", "Objawy", "objaw"),
    MEASUREMENTS("measurements", "app/measurements", "Pomiary", "pomiar"),
    DOCTORS("doctors", "app/doctors", "Lekarze", "lekarza"),
    APPOINTMENTS("appointments", "app/appointments", "Wizyty", "wizytę"),
    ;

    companion object {
        fun fromRoute(route: String?): ResourceType =
            entries.firstOrNull { it.route == route } ?: ALLERGIES
    }
}

enum class FieldType {
    TEXT,
    MULTILINE,
    DECIMAL,
    INTEGER,
    DATE,
    DATETIME,
    ENUM,
}

data class FormField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val defaultValue: String = "",
    val hint: String? = null,
    /** false for server-managed values that must not be sent as null. */
    val clearable: Boolean = true,
)

data class RecordSummary(
    val title: String,
    val subtitle: String,
    val status: String? = null,
)

object ResourceCatalog {

    fun fields(type: ResourceType): List<FormField> = when (type) {
        ResourceType.ALLERGIES -> listOf(
            FormField("substance", "Substancja", FieldType.TEXT, required = true),
            FormField("reaction", "Reakcja", FieldType.MULTILINE),
            FormField(
                "severity", "Nasilenie", FieldType.ENUM,
                options = listOf("MILD", "MODERATE", "SEVERE", "LIFE_THREATENING", "UNKNOWN"),
                defaultValue = "UNKNOWN",
            ),
            FormField(
                "status", "Status", FieldType.ENUM,
                options = listOf("ACTIVE", "RESOLVED", "SUSPECTED"),
                defaultValue = "ACTIVE",
            ),
            FormField("diagnosedAt", "Data diagnozy", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField("notes", "Notatki", FieldType.MULTILINE),
        )

        ResourceType.CONDITIONS -> listOf(
            FormField("name", "Nazwa schorzenia", FieldType.TEXT, required = true),
            FormField("description", "Opis", FieldType.MULTILINE),
            FormField(
                "status", "Status", FieldType.ENUM,
                options = listOf("ACTIVE", "IN_REMISSION", "RESOLVED", "SUSPECTED"),
                defaultValue = "ACTIVE",
            ),
            FormField("onsetDate", "Początek", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField("resolvedDate", "Zakończenie", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField(
                "origin", "Pochodzenie", FieldType.ENUM,
                options = listOf("PATIENT_REPORTED", "CLINICIAN_DOCUMENTED", "IMPORTED"),
                defaultValue = "PATIENT_REPORTED",
            ),
        )

        ResourceType.MEDICATIONS -> listOf(
            FormField("name", "Nazwa leku", FieldType.TEXT, required = true),
            FormField("activeIngredient", "Substancja czynna", FieldType.TEXT),
            FormField("strength", "Moc", FieldType.TEXT, hint = "np. 500 mg"),
            FormField("dosageForm", "Postać", FieldType.TEXT, hint = "np. tabletka"),
            FormField("prescribedDose", "Dawka", FieldType.TEXT, hint = "np. 1 tabletka"),
            FormField("route", "Droga podania", FieldType.TEXT, hint = "np. doustnie"),
            FormField("frequency", "Częstotliwość", FieldType.TEXT, hint = "np. 2 razy dziennie"),
            FormField("instructions", "Instrukcje", FieldType.MULTILINE),
            FormField("startDate", "Od", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField("endDate", "Do", FieldType.DATE, hint = "RRRR-MM-DD"),
            FormField(
                "status", "Status", FieldType.ENUM,
                options = listOf("ACTIVE", "PAUSED", "STOPPED", "COMPLETED"),
                defaultValue = "ACTIVE",
            ),
            FormField("notes", "Notatki", FieldType.MULTILINE),
        )

        ResourceType.SYMPTOMS -> listOf(
            FormField("title", "Tytuł", FieldType.TEXT, required = true),
            FormField("description", "Opis", FieldType.MULTILINE),
            FormField("bodyLocation", "Lokalizacja", FieldType.TEXT, hint = "np. głowa"),
            FormField("severity", "Nasilenie 1–10", FieldType.INTEGER),
            FormField("startedAt", "Początek", FieldType.DATETIME, hint = "RRRR-MM-DD GG:MM"),
            FormField("endedAt", "Koniec", FieldType.DATETIME, hint = "RRRR-MM-DD GG:MM"),
            FormField(
                "observedAt", "Czas obserwacji", FieldType.DATETIME,
                hint = "puste = teraz", clearable = false,
            ),
            FormField(
                "status", "Status", FieldType.ENUM,
                options = listOf("ACTIVE", "RESOLVED", "MONITORING"),
                defaultValue = "ACTIVE",
            ),
        )

        ResourceType.MEASUREMENTS -> listOf(
            FormField(
                "type", "Typ", FieldType.ENUM, required = true,
                options = listOf(
                    "BLOOD_PRESSURE", "HEART_RATE", "WEIGHT", "TEMPERATURE",
                    "GLUCOSE", "OXYGEN_SATURATION", "OTHER",
                ),
                defaultValue = "BLOOD_PRESSURE",
            ),
            FormField("value", "Wartość", FieldType.DECIMAL, required = true),
            FormField("secondaryValue", "Wartość dodatkowa", FieldType.DECIMAL, hint = "np. rozkurczowe"),
            FormField("unit", "Jednostka", FieldType.TEXT, required = true, hint = "np. mmHg"),
            FormField(
                "measuredAt", "Data pomiaru", FieldType.DATETIME, required = true,
                hint = "RRRR-MM-DD GG:MM",
            ),
            FormField(
                "source", "Źródło", FieldType.ENUM,
                options = listOf("MANUAL", "DEVICE", "IMPORTED", "UNKNOWN"),
                defaultValue = "MANUAL",
            ),
            FormField("notes", "Notatki", FieldType.MULTILINE),
        )

        ResourceType.DOCTORS -> listOf(
            FormField("firstName", "Imię", FieldType.TEXT, required = true),
            FormField("lastName", "Nazwisko", FieldType.TEXT, required = true),
            FormField("specialty", "Specjalizacja", FieldType.TEXT),
            FormField("facilityName", "Placówka", FieldType.TEXT),
            FormField("phone", "Telefon", FieldType.TEXT),
            FormField("email", "Email", FieldType.TEXT),
            FormField("address", "Adres", FieldType.TEXT),
            FormField("notes", "Notatki", FieldType.MULTILINE),
        )

        ResourceType.APPOINTMENTS -> listOf(
            FormField(
                "scheduledAt", "Termin", FieldType.DATETIME, required = true,
                hint = "RRRR-MM-DD GG:MM",
            ),
            FormField("endedAt", "Koniec", FieldType.DATETIME, hint = "RRRR-MM-DD GG:MM"),
            FormField("location", "Miejsce", FieldType.TEXT),
            FormField("reason", "Powód", FieldType.TEXT, required = true),
            FormField(
                "status", "Status", FieldType.ENUM,
                options = listOf("SCHEDULED", "COMPLETED", "CANCELLED", "MISSED"),
                defaultValue = "SCHEDULED",
            ),
            FormField("notes", "Notatki", FieldType.MULTILINE),
            FormField("recommendations", "Zalecenia", FieldType.MULTILINE),
        )
    }

    fun summary(type: ResourceType, json: com.google.gson.JsonObject): RecordSummary {
        fun text(key: String): String? = json.get(key)
            ?.takeUnless { it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }

        fun number(key: String): String? = json.get(key)
            ?.takeUnless { it.isJsonNull }
            ?.asString
            ?.takeIf { it.isNotBlank() }

        return when (type) {
            ResourceType.ALLERGIES -> RecordSummary(
                title = text("substance") ?: "Alergia",
                subtitle = listOfNotNull(
                    text("reaction"),
                    text("severity")?.let(::prettyEnum),
                    text("diagnosedAt"),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = text("status")?.let(::prettyEnum),
            )

            ResourceType.CONDITIONS -> RecordSummary(
                title = text("name") ?: "Schorzenie",
                subtitle = listOfNotNull(
                    text("onsetDate")?.let { "od $it" },
                    text("origin")?.let(::prettyEnum),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = text("status")?.let(::prettyEnum),
            )

            ResourceType.MEDICATIONS -> RecordSummary(
                title = listOfNotNull(text("name"), text("strength")).joinToString(" "),
                subtitle = listOfNotNull(
                    text("prescribedDose"),
                    text("frequency"),
                    text("route"),
                ).joinToString(" • ").ifBlank { "Brak dawkowania" },
                status = text("status")?.let(::prettyEnum),
            )

            ResourceType.SYMPTOMS -> RecordSummary(
                title = text("title") ?: "Objaw",
                subtitle = listOfNotNull(
                    text("observedAt")?.let(::formatDateTime),
                    text("severity")?.let { "nasilenie $it/10" },
                    text("bodyLocation"),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = text("status")?.let(::prettyEnum),
            )

            ResourceType.MEASUREMENTS -> RecordSummary(
                title = buildString {
                    append(text("type")?.let(::prettyEnum) ?: "Pomiar")
                    append(": ")
                    append(number("value") ?: "?")
                    text("secondaryValue")?.let { append("/$it") }
                    append(" ")
                    append(text("unit") ?: "")
                }.trim(),
                subtitle = listOfNotNull(
                    text("measuredAt")?.let(::formatDateTime),
                    text("source")?.let(::prettyEnum),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = null,
            )

            ResourceType.DOCTORS -> RecordSummary(
                title = listOfNotNull(text("firstName"), text("lastName")).joinToString(" ")
                    .ifBlank { "Lekarz" },
                subtitle = listOfNotNull(
                    text("specialty"),
                    text("facilityName"),
                    text("phone"),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = null,
            )

            ResourceType.APPOINTMENTS -> RecordSummary(
                title = text("reason") ?: "Wizyta",
                subtitle = listOfNotNull(
                    text("scheduledAt")?.let(::formatDateTime),
                    text("location"),
                ).joinToString(" • ").ifBlank { "Brak szczegółów" },
                status = text("status")?.let(::prettyEnum),
            )
        }
    }

    fun prettyEnum(value: String): String = when (value) {
        "ACTIVE" -> "Aktywny"
        "RESOLVED" -> "Rozwiązany"
        "SUSPECTED" -> "Podejrzewany"
        "MONITORING" -> "Obserwacja"
        "IN_REMISSION" -> "Remisja"
        "PATIENT_REPORTED" -> "Zgłoszone przez pacjenta"
        "CLINICIAN_DOCUMENTED" -> "Potwierdzone przez lekarza"
        "IMPORTED" -> "Zaimportowane"
        "PAUSED" -> "Wstrzymany"
        "STOPPED" -> "Odstawiony"
        "COMPLETED" -> "Zakończony"
        "SCHEDULED" -> "Zaplanowana"
        "CANCELLED" -> "Odwołana"
        "MISSED" -> "Nieodbyta"
        "MILD" -> "Łagodne"
        "MODERATE" -> "Umiarkowane"
        "SEVERE" -> "Ciężkie"
        "LIFE_THREATENING" -> "Zagrażające życiu"
        "UNKNOWN" -> "Nieznane"
        "BLOOD_PRESSURE" -> "Ciśnienie"
        "HEART_RATE" -> "Tętno"
        "WEIGHT" -> "Waga"
        "TEMPERATURE" -> "Temperatura"
        "GLUCOSE" -> "Glukoza"
        "OXYGEN_SATURATION" -> "Saturacja"
        "OTHER" -> "Inne"
        "MANUAL" -> "Ręczny"
        "DEVICE" -> "Urządzenie"
        "FEMALE" -> "Kobieta"
        "MALE" -> "Mężczyzna"
        "LAB_RESULT" -> "Wynik badania"
        "DISCHARGE_SUMMARY" -> "Wypis ze szpitala"
        "PRESCRIPTION" -> "Recepta"
        "REFERRAL" -> "Skierowanie"
        "IMAGING" -> "Badanie obrazowe"
        "SKIN" -> "Skóra"
        "INJURY" -> "Uraz"
        "MEDICATION_PACKAGE" -> "Opakowanie leku"
        "DOCUMENT" -> "Dokument"
        "A_POSITIVE" -> "A Rh+"
        "A_NEGATIVE" -> "A Rh−"
        "B_POSITIVE" -> "B Rh+"
        "B_NEGATIVE" -> "B Rh−"
        "AB_POSITIVE" -> "AB Rh+"
        "AB_NEGATIVE" -> "AB Rh−"
        "ZERO_POSITIVE" -> "0 Rh+"
        "ZERO_NEGATIVE" -> "0 Rh−"
        else -> value.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun formatDateTime(iso: String): String = DateInputs.toDisplay(iso)
}
