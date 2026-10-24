package com.patientcard.timeline

import java.time.Instant
import java.util.UUID

/** Rodzaj zdarzenia na wspólnej osi czasu. */
enum class TimelineEventType {
    APPOINTMENT,
    MEASUREMENT,
    SYMPTOM,
    MEDICATION,
    DOCUMENT,
    IMAGE,
}

/**
 * Jedno zdarzenie w chronologii pacjenta.
 *
 * @param occurredAt moment, w którym zdarzenie miało miejsce medycznie
 *   (np. data wizyty, data pomiaru), a nie moment dodania wpisu.
 */
data class TimelineEventResponse(
    val id: UUID,
    val type: TimelineEventType,
    val occurredAt: Instant,
    val title: String,
    val subtitle: String?,
    val status: String?,
    val resourceId: UUID,
)
