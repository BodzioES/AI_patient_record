package com.example.ar_project_1.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.example.ar_project_1.data.model.AiStatusDto

/**
 * Status dostawcy AI pobrany z backendu (`GET /api/v1/ai/status`).
 * `null` = jeszcze nie wiadomo (np. brak połączenia), więc UI nie powinno
 * twierdzić, że działa prawdziwy model.
 */
val LocalAiStatus = compositionLocalOf<AiStatusDto?> { null }

/** Czy na pewno wiemy, że odpowiedzi są demonstracyjne. */
fun aiIsMock(status: AiStatusDto?): Boolean = status?.mock ?: true

/** Nazwa modelu do pokazania w UI albo "MOCK". */
fun aiModeLabel(status: AiStatusDto?): String = when {
    status == null -> "…"
    status.mock -> "MOCK"
    else -> status.modelName.ifBlank { "model lokalny" }
}
