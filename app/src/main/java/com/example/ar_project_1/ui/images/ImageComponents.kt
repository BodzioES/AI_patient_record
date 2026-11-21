package com.example.ar_project_1.ui.images

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.FieldType
import com.example.ar_project_1.data.resource.FormField
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.upload.PickedFile
import com.example.ar_project_1.data.upload.describe
import com.example.ar_project_1.ui.common.EnumDropdown
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.RemoteImage
import com.example.ar_project_1.ui.common.RemoteImageLoader
import java.util.Locale

@Composable
internal fun ImageCard(
    image: ImageDto,
    loader: RemoteImageLoader,
    busy: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                imageId = image.id,
                loader = loader,
                contentDescription = image.title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    image.title ?: "Zdjęcie #${image.imageNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    listOfNotNull(
                        ResourceCatalog.prettyEnum(image.category),
                        image.widthPx?.let { w -> image.heightPx?.let { h -> "${w}×$h px" } },
                        formatSize(image.fileSizeBytes),
                        (image.capturedAt ?: image.uploadedAt)?.let(DateInputs::toDisplay),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!image.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(image.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń")
                }
            }
        }
    }
}

@Composable
internal fun UploadImageDialog(
    uploading: Boolean,
    uploadError: String?,
    uploadCompleted: Boolean,
    onDismiss: () -> Unit,
    onUpload: (PickedFile, ImageUploadForm) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var pickError by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf(ImageUploadForm()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

    @Composable
    fun field(label: String, value: String, placeholder: String? = null, lines: Int = 1, onChange: (String) -> Unit) =
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = lines == 1,
            minLines = lines,
            enabled = !uploading,
            modifier = Modifier.fillMaxWidth(),
        )

    AlertDialog(
        onDismissRequest = { if (!uploading) onDismiss() },
        title = { Text("Nowe zdjęcie") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(arrayOf("image/*"))
                    },
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (picked == null) "Wybierz zdjęcie" else "Zmień zdjęcie")
                }
                picked?.let {
                    Text(
                        "${it.name} • ${formatSize(it.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                pickError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                EnumDropdown(
                    field = FormField(
                        key = "category",
                        label = "Kategoria",
                        type = FieldType.ENUM,
                        required = true,
                        options = ImagesViewModel.CATEGORIES,
                    ),
                    value = form.category,
                    onChange = { form = form.copy(category = it) },
                )
                field("Tytuł", form.title) { form = form.copy(title = it) }
                field("Opis", form.description, lines = 2) { form = form.copy(description = it) }
                field("Data wykonania", form.capturedAt, "RRRR-MM-DD GG:MM") { form = form.copy(capturedAt = it) }
                field("ID objawu (opcjonalnie)", form.symptomId) { form = form.copy(symptomId = it) }
                if (uploadError != null) ErrorBanner(uploadError)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = picked
                    if (file == null) pickError = "Wybierz zdjęcie do wysłania." else onUpload(file, form)
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

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f kB", bytes / 1024.0)
    else -> "$bytes B"
}
