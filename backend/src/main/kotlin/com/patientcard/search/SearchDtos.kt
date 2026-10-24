package com.patientcard.search

import java.time.Instant
import java.util.UUID

enum class SearchHitType {
    ALLERGY,
    CONDITION,
    MEDICATION,
    SYMPTOM,
    MEASUREMENT,
    DOCTOR,
    APPOINTMENT,
    DOCUMENT,
    IMAGE,
}

/** Wynik wyszukiwania w całej karcie pacjenta. */
data class SearchHitResponse(
    val id: UUID,
    val type: SearchHitType,
    val title: String,
    val subtitle: String?,
    val occurredAt: Instant?,
)
