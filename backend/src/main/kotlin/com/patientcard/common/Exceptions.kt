package com.patientcard.common

import org.springframework.http.HttpStatus

open class ApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
) : RuntimeException(message)

class NotFoundException(message: String = "Nie znaleziono zasobu.") :
    ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)

class ConflictException(message: String = "Zasób już istnieje.") :
    ApiException(HttpStatus.CONFLICT, "CONFLICT", message)

class BadRequestException(message: String, val errors: Map<String, String> = emptyMap()) :
    ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message)

class UnauthorizedException(message: String = "Wymagane uwierzytelnienie.") :
    ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message)

class FileValidationException(message: String) :
    ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_FILE", message)

class PayloadTooLargeException(message: String) :
    ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", message)

class StorageException(message: String, cause: Throwable? = null) :
    ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", message) {
    init {
        if (cause != null) addSuppressed(cause)
    }
}
