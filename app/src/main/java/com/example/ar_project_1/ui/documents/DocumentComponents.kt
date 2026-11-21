package com.example.ar_project_1.ui.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.FieldType
import com.example.ar_project_1.data.resource.FormField
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.upload.PickedFile
import com.example.ar_project_1.data.upload.describe
import com.example.ar_project_1.ui.common.EnumDropdown
import com.example.ar_project_1.ui.common.ErrorBanner
import java.util.Locale

@Composable
internal fun DocumentCard(
    document: DocumentDto,
    busy: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    document.originalFilename,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    listOfNotNull(
                        ResourceCatalog.prettyEnum(document.documentType),
                        formatBytes(document.fileSizeBytes),
                        document.documentDate,
                        document.uploadedAt?.let(DateInputs::toDisplay),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!document.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        document.description,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.Download, contentDescription = "Pobierz i otwórz")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń")
                }
            }
        }
    }
}

@Composable
internal fun UploadDocumentDialog(
    uploading: Boolean,
    uploadError: String?,
    uploadCompleted: Boolean,
    onDismiss: () -> Unit,
    onUpload: (PickedFile, String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var pickError by remember { mutableStateOf<String?>(null) }
    var documentType by remember { mutableStateOf(DocumentsViewModel.DOCUMENT_TYPES.first()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var documentDate by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.describe(uri) }
                .onSuccess {
                    picked = it
                    pickError = null
                }
                .onFailure { pickError = it.message ?: "Nie można odczytać pliku." }
        }
    }

    LaunchedEffect(uploadCompleted) {
        if (uploadCompleted) onDismiss()
    }

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text("Nowy dokument") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/pdf", "image/*")) },
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (picked == null) "Wybierz plik (PDF/JPG/PNG)" else "Zmień plik")
                }
                picked?.let {
                    Text(
                        "${it.name} • ${formatBytes(it.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                pickError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                EnumDropdown(
                    field = FormField(
                        key = "documentType",
                        label = "Typ dokumentu",
                        type = FieldType.ENUM,
                        required = true,
                        options = DocumentsViewModel.DOCUMENT_TYPES,
                    ),
                    value = documentType,
                    onChange = { documentType = it },
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł *") },
                    singleLine = true,
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = documentDate,
                    onValueChange = { documentDate = it },
                    label = { Text("Data dokumentu") },
                    placeholder = { Text("RRRR-MM-DD") },
                    singleLine = true,
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uploadError != null) ErrorBanner(uploadError)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = picked
                    if (file == null) {
                        pickError = "Wybierz plik do wysłania."
                    } else {
                        onUpload(file, documentType, title, description, documentDate)
                    }
                },
                enabled = !uploading,
            ) {
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Wyślij")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uploading) { Text("Anuluj") }
        },
    )
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f kB", bytes / 1024.0)
    else -> "$bytes B"
}
