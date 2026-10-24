package com.example.ar_project_1.data.upload

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileSaver(private val context: Context) {

    suspend fun save(bytes: ByteArray, fileName: String, mimeType: String): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val safeName = File(fileName).name.ifBlank { "plik" }
        val file = File(directory, safeName)
        file.writeBytes(bytes)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun open(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        File(context.cacheDir, "shared").deleteRecursively()
    }
}
