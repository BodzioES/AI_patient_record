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

/**
 * Validates uploads by their content (magic bytes), not by what the client claims.
 *
 * Android pickers often deliver files without an extension (`IMG_0001`), with an
 * alias MIME type (`image/jpg`) or with `application/octet-stream`. Those are
 * accepted as long as the bytes are a supported format; a client that declares a
 * *different* supported format than the content (e.g. `.png` holding a PDF) is
 * still rejected.
 */
@Component
class FileTypeValidator(appProperties: AppProperties) {

    private val properties = appProperties.storage

    private val imageTypes = setOf("jpg", "png", "webp")
    private val documentTypes = imageTypes + "pdf"

    fun validateImage(file: MultipartFile): DetectedFileType = validate(file, imageTypes)

    fun validateDocument(file: MultipartFile): DetectedFileType = validate(file, documentTypes)

    private fun validate(file: MultipartFile, allowed: Set<String>): DetectedFileType {
        if (file.isEmpty || file.size <= 0) {
            throw FileValidationException("Plik jest pusty.")
        }
        if (file.size > properties.maxFileSizeBytes) {
            throw PayloadTooLargeException("Maksymalny rozmiar pliku to ${properties.maxFileSizeBytes} bajtów.")
        }
        val detected = detect(readHeader(file))
            ?: throw FileValidationException("Zawartość pliku nie odpowiada obsługiwanemu formatowi.")
        if (detected.extension !in allowed) {
            throw FileValidationException(
                "Niedozwolony typ pliku. Dozwolone: ${allowed.joinToString(", ")}.",
            )
        }

        val claimedExtension = normalizeExtension(
            file.originalFilename?.substringAfterLast('.', "")?.trim()?.lowercase(),
        )
        if (claimedExtension in KNOWN_EXTENSIONS && claimedExtension != detected.extension) {
            throw FileValidationException("Rozszerzenie pliku nie odpowiada jego zawartości.")
        }
        val declared = normalizeMime(file.contentType)
        val declaredExtension = MIME_TO_EXTENSION[declared]
        if (declaredExtension != null && declaredExtension != detected.extension) {
            throw FileValidationException("Deklarowany typ MIME nie pasuje do zawartości pliku.")
        }
        return detected
    }

    private fun readHeader(file: MultipartFile): ByteArray = file.inputStream.use { input ->
        val buffer = ByteArray(HEADER_SIZE)
        var total = 0
        while (total < HEADER_SIZE) {
            val read = input.read(buffer, total, HEADER_SIZE - total)
            if (read <= 0) break
            total += read
        }
        buffer.copyOf(total)
    }

    private fun normalizeExtension(extension: String?): String? = when (extension) {
        "jpeg", "jpe", "jfif" -> "jpg"
        else -> extension
    }

    private fun normalizeMime(mime: String?): String? =
        mime?.lowercase()?.substringBefore(';')?.trim()?.ifEmpty { null }

    companion object {
        private const val HEADER_SIZE = 16
        private val KNOWN_EXTENSIONS = setOf("jpg", "png", "webp", "pdf")
        private val MIME_TO_EXTENSION = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/pjpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "application/pdf" to "pdf",
        )

        fun detect(bytes: ByteArray): DetectedFileType? {
            fun at(index: Int, value: Int) = bytes.size > index && bytes[index] == value.toByte()
            fun ascii(offset: Int, text: String) = text.withIndex().all { (i, c) -> at(offset + i, c.code) }
            return when {
                at(0, 0xFF) && at(1, 0xD8) && at(2, 0xFF) -> DetectedFileType("jpg", "image/jpeg")
                at(0, 0x89) && ascii(1, "PNG") && at(4, 0x0D) && at(5, 0x0A) && at(6, 0x1A) && at(7, 0x0A) ->
                    DetectedFileType("png", "image/png")
                ascii(0, "RIFF") && ascii(8, "WEBP") -> DetectedFileType("webp", "image/webp")
                ascii(0, "%PDF") -> DetectedFileType("pdf", "application/pdf")
                else -> null
            }
        }
    }
}
