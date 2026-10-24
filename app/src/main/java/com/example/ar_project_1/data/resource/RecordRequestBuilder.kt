package com.example.ar_project_1.data.resource

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.math.BigDecimal

object RecordRequestBuilder {

    fun build(
        type: ResourceType,
        values: Map<String, String>,
        isUpdate: Boolean,
    ): JsonObject {
        val json = JsonObject()
        ResourceCatalog.fields(type).forEach { field ->
            val raw = values[field.key]?.trim().orEmpty()
            if (raw.isEmpty()) {
                if (field.required) {
                    throw IllegalArgumentException("Pole „${field.label}” jest wymagane.")
                }
                if (isUpdate && field.clearable) {
                    json.add(field.key, JsonNull.INSTANCE)
                }
                return@forEach
            }

            val value = when (field.type) {
                FieldType.TEXT, FieldType.MULTILINE, FieldType.ENUM -> JsonPrimitive(raw)
                FieldType.DECIMAL -> {
                    val number = raw.replace(',', '.').toBigDecimalOrNull()
                        ?: throw IllegalArgumentException("Pole „${field.label}” musi być liczbą.")
                    JsonPrimitive(number)
                }

                FieldType.INTEGER -> {
                    val number = raw.toIntOrNull()
                        ?: throw IllegalArgumentException("Pole „${field.label}” musi być liczbą całkowitą.")
                    JsonPrimitive(number)
                }

                FieldType.DATE -> {
                    if (!DATE_PATTERN.matches(raw)) {
                        throw IllegalArgumentException("Pole „${field.label}” musi mieć format RRRR-MM-DD.")
                    }
                    JsonPrimitive(raw)
                }

                FieldType.DATETIME -> JsonPrimitive(DateInputs.toInstantString(raw))
            }
            json.add(field.key, value)
        }
        return json
    }

    fun prefill(type: ResourceType, json: JsonObject): Map<String, String> {
        return ResourceCatalog.fields(type).associate { field ->
            val element = json.get(field.key)?.takeUnless { it.isJsonNull }
            val raw = element?.asString.orEmpty()
            val value = when (field.type) {
                FieldType.DATETIME -> DateInputs.toFormValue(raw)
                FieldType.DECIMAL -> raw.replace(',', '.')
                else -> raw
            }
            field.key to value
        }
    }

    fun initialValues(type: ResourceType): Map<String, String> =
        ResourceCatalog.fields(type).associate { it.key to it.defaultValue }

    fun validateRequired(type: ResourceType, values: Map<String, String>) {
        val missing = ResourceCatalog.fields(type)
            .firstOrNull { it.required && values[it.key].isNullOrBlank() }
        if (missing != null) {
            throw IllegalArgumentException("Pole „${missing.label}” jest wymagane.")
        }
    }

    fun decimalOrNull(value: String): BigDecimal? =
        value.trim().replace(',', '.').toBigDecimalOrNull()

    private val DATE_PATTERN = Regex("^\\d{4}-\\d{2}-\\d{2}$")
}
