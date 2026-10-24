package com.example.ar_project_1.data.upload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

data class PickedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
)

fun ContentResolver.describe(uri: Uri): PickedFile {
    var name = "plik"
    var size = -1L
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    val mime = getType(uri) ?: "application/octet-stream"
    if (size <= 0) {
        size = openInputStream(uri)?.use { input ->
            var total = 0L
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
            }
            total
        } ?: 0L
    }
    return PickedFile(uri, name, mime, size)
}

class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val picked: PickedFile,
) : RequestBody() {
    override fun contentType() = picked.mimeType.toMediaTypeOrNull()
    override fun contentLength(): Long = picked.sizeBytes
    override fun writeTo(sink: BufferedSink) {
        resolver.openInputStream(picked.uri)?.use { input ->
            sink.writeAll(input.source())
        } ?: throw IllegalStateException("Nie można odczytać wybranego pliku.")
    }
}
