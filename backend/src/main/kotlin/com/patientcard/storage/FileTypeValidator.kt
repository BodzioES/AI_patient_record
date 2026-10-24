package com.patientcard.storage

import com.patientcard.common.FileValidationException
import com.patientcard.common.PayloadTooLargeException
import com.patientcard.config.AppProperties
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

data class DetectedFileType(
    val extension: String,
    val mimeType: String,
)

@Component
class FileTypeValidator(appProperties: AppProperties) {

    private val properties = appProperties.storage

    private val imageTypes = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
    )

    private val documentTypes = imageTypes + mapOf("pdf" to "application/pdf")

    fun validateImage(file: MultipartFile): DetectedFileType = validate(file, imageTypes)

    fun validateDocument(file: MultipartFile): DetectedFileType = validate(file, documentTypes)

    private fun validate(file: MultipartFile, allowed: Map<String, String>): DetectedFileType {
        if (file.isEmpty) {
            throw FileValidationException("Plik jest pusty.")
        }
        if (file.size <= 0 || file.size > properties.maxFileSizeBytes) {
            throw PayloadTooLargeException("Maksymalny rozmiar pliku to ${properties.maxFileSizeBytes} bajtów.")
        }
        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.trim()
            ?.lowercase()
            ?: ""
        if (extension.isEmpty() || extension !in allowed) {
            throw FileValidationException("Niedozwolone rozszerzenie pliku. Dozwolone: ${allowed.keys.joinToString(", ")}.")
        }
        val declared = file.contentType?.lowercase()?.substringBefore(';')?.trim()
        if (declared != null && declared != "application/octet-stream" && declared != allowed[extension]) {
            throw FileValidationException("Deklarowany typ MIME nie pasuje do rozszerzenia pliku.")
        }
        val bytes = file.bytes
        val detected = detect(bytes)
            ?: throw FileValidationException("Zawartość pliku nie odpowiada obsługiwanemu formatowi.")
        if (detected.extension !in allowed) {
            throw FileValidationException("Zawartość pliku nie odpowiada dozwolonemu formatowi.")
        }
        if (detected.extension != extension && !(detected.extension == "jpg" && extension == "jpeg")) {
            throw FileValidationException("Rozszerzenie pliku nie odpowiada jego zawartości.")
        }
        return DetectedFileType(extension = extension, mimeType = detected.mimeType)
    }

    private fun detect(bytes: ByteArray): DetectedFileType? {
        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return DetectedFileType("jpg", "image/jpeg")
        }
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
        ) {
            return DetectedFileType("png", "image/png")
        }
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return DetectedFileType("webp", "image/webp")
        }
        if (bytes.size >= 5 &&
            bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()
        ) {
            return DetectedFileType("pdf", "application/pdf")
        }
        return null
    }
}
