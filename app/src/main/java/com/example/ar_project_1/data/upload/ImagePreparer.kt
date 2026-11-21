package com.example.ar_project_1.data.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Makes a picked photo acceptable for the backend before upload.
 *
 * Gallery pickers return names without an extension, alias MIME types or HEIC
 * photos (default on many phones). The backend accepts JPG/PNG/WEBP, so the
 * real format is sniffed from the bytes: supported files get a matching name and
 * MIME type, anything else is decoded and re-encoded as JPEG in the cache.
 */
class ImagePreparer(private val context: Context) {

    private val resolver = context.contentResolver
    private val directory: File get() = File(context.cacheDir, "upload").apply { mkdirs() }

    suspend fun prepare(picked: PickedFile): PickedFile = withContext(Dispatchers.IO) {
        val format = sniff(picked.uri)
        if (format != null) {
            picked.copy(name = withExtension(picked.name, format.extension), mimeType = format.mime)
        } else {
            transcodeToJpeg(picked)
        }
    }

    /** Removes converted copies left over from previous uploads. */
    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun sniff(uri: Uri): Format? {
        val header = ByteArray(12)
        val read = resolver.openInputStream(uri)?.use { input ->
            var total = 0
            while (total < header.size) {
                val count = input.read(header, total, header.size - total)
                if (count <= 0) break
                total += count
            }
            total
        } ?: throw IllegalStateException("Nie można odczytać wybranego pliku.")
        fun at(i: Int, v: Int) = read > i && header[i] == v.toByte()
        fun ascii(offset: Int, text: String) = text.withIndex().all { (i, c) -> at(offset + i, c.code) }
        return when {
            at(0, 0xFF) && at(1, 0xD8) && at(2, 0xFF) -> Format.JPEG
            at(0, 0x89) && ascii(1, "PNG") -> Format.PNG
            ascii(0, "RIFF") && ascii(8, "WEBP") -> Format.WEBP
            else -> null
        }
    }

    private fun transcodeToJpeg(picked: PickedFile): PickedFile {
        val source = ImageDecoder.createSource(resolver, picked.uri)
        val bitmap = try {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE) decoder.setTargetSampleSize(longest / MAX_SIDE)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Nieobsługiwany format zdjęcia. Wybierz plik JPG, PNG lub WEBP.", e)
        }
        val target = File(directory, "${UUID.randomUUID()}.jpg")
        try {
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        } finally {
            bitmap.recycle()
        }
        return PickedFile(
            uri = Uri.fromFile(target),
            name = withExtension(picked.name, "jpg"),
            mimeType = Format.JPEG.mime,
            sizeBytes = target.length(),
        )
    }

    private fun withExtension(name: String, extension: String): String {
        val base = name.substringAfterLast('/').substringBeforeLast('.').trim().ifEmpty { "zdjecie" }
        return "$base.$extension"
    }

    private enum class Format(val extension: String, val mime: String) {
        JPEG("jpg", "image/jpeg"),
        PNG("png", "image/png"),
        WEBP("webp", "image/webp"),
    }

    private companion object {
        const val MAX_SIDE = 4096
        const val JPEG_QUALITY = 92
    }
}
