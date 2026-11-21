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
