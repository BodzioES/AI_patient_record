package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.model.ApiProblem
import com.google.gson.Gson
import java.io.IOException

class AppException(
    override val message: String,
    val code: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val status: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

object ApiErrorParser {

    private val gson = Gson()

    fun parse(status: Int, body: String?): AppException {
        val problem = body
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { gson.fromJson(it, ApiProblem::class.java) }.getOrNull()
            }
        val detail = problem?.detail?.takeIf { it.isNotBlank() }
            ?: defaultMessage(status)
        return AppException(
            message = detail,
            code = problem?.code,
            fieldErrors = problem?.errors.orEmpty(),
            status = status,
        )
    }

    fun network(exception: IOException): AppException = AppException(
        message = "Brak połączenia z serwerem. Sprawdź adres, Wi-Fi i czy backend działa.",
        code = "NETWORK_ERROR",
        cause = exception,
    )

    private fun defaultMessage(status: Int): String = when (status) {
        400 -> "Dane są nieprawidłowe."
        401 -> "Sesja wygasła. Zaloguj się ponownie."
        403 -> "Brak uprawnień."
        404 -> "Nie znaleziono zasobu."
        409 -> "Taki wpis już istnieje."
        413 -> "Plik jest zbyt duży."
        415 -> "Niedozwolony typ pliku."
        503 -> "Usługa AI jest chwilowo niedostępna."
        else -> "Wystąpił błąd serwera ($status)."
    }
}
