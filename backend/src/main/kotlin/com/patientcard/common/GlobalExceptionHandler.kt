package com.patientcard.common

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ProblemDetail> {
        val problem = problem(ex.status, ex.code, ex.message)
        if (ex is BadRequestException && ex.errors.isNotEmpty()) {
            problem.setProperty("errors", ex.errors)
        }
        return ResponseEntity.status(ex.status).body(problem)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Nieprawidłowa wartość.") }
        val problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dane wejściowe są nieprawidłowe.")
        problem.setProperty("errors", errors)
        return ResponseEntity.badRequest().body(problem)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
        ResponseEntity.badRequest().body(
            problem(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Nie można odczytać treści żądania."),
        )

    @ExceptionHandler(
        MissingServletRequestPartException::class,
        MissingServletRequestParameterException::class,
    )
    fun handleMissingPart(ex: Exception): ResponseEntity<ProblemDetail> =
        ResponseEntity.badRequest().body(
            problem(HttpStatus.BAD_REQUEST, "MISSING_PART", "Brakuje wymaganego pola lub pliku."),
        )

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(ex: MaxUploadSizeExceededException): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            problem(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Plik jest zbyt duży."),
        )

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ProblemDetail> {
        logger.warn("Data integrity violation: {}", ex.mostSpecificCause.javaClass.simpleName)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "Operacja narusza ograniczenia danych."),
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "Brak uprawnień do tego zasobu."),
        )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Nie znaleziono zasobu."),
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ProblemDetail> {
        logger.error("Unexpected server error ({}): {}", ex.javaClass.simpleName, ex.message)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Wystąpił nieoczekiwany błąd serwera."),
        )
    }

    private fun problem(status: HttpStatusCode, code: String, detail: String): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = when (status.value()) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            409 -> "Conflict"
            413 -> "Payload Too Large"
            415 -> "Unsupported Media Type"
            else -> "Internal Server Error"
        }
        problem.setProperty("code", code)
        return problem
    }
}
