package com.example.ar_project_1.ui.documents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.FieldType
import com.example.ar_project_1.data.resource.FormField
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.upload.PickedFile
import com.example.ar_project_1.data.upload.describe
import com.example.ar_project_1.ui.common.ConfirmDeleteDialog
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.EnumDropdown
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox
import java.util.Locale

@Composable
fun DocumentsScreen(container: AppContainer) {
    val viewModel: DocumentsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DocumentsViewModel(container.repository, container.fileSaver) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    var showUpload by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DocumentDto?>(null) }
    var sortExpanded by remember { mutableStateOf(false) }
    val currentSort = DocumentsViewModel.SORTS.firstOrNull { it.second == state.sort }
        ?: DocumentsViewModel.SORTS.first()

    if (state.loading && state.documents.isEmpty()) {
        LoadingBox(label = "Pobieranie dokumentów…")
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Dokumenty (${state.documents.size}/${state.total})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = viewModel::load, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Sortowanie:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Text(currentSort.first)
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                        ) {
                            DocumentsViewModel.SORTS.forEach { (label, value) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        sortExpanded = false
                                        viewModel.setSort(value)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (state.error != null) {
                item { ErrorBanner(state.error, onRetry = viewModel::load) }
            }
            if (state.message != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            state.message,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            if (state.documents.isEmpty()) {
                item {
                    EmptyState(
                        title = "Brak dokumentów",
                        message = "Dodaj wynik badania, wypis ze szpitala lub skierowanie.",
                    )
                }
            } else {
                items(state.documents, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        busy = state.busyId == document.id,
                        onOpen = { viewModel.download(document) },
                        onDelete = { pendingDelete = document },
                    )
                }
            }
            if (!state.last) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.loadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            OutlinedButton(onClick = viewModel::loadMore) {
                                Text("Pokaż więcej (${state.documents.size}/${state.total})")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = { showUpload = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj dokument")
        }
    }

    if (showUpload) {
        UploadDocumentDialog(
            uploading = state.uploading,
            uploadError = state.uploadError,
            uploadCompleted = state.uploadCompleted,
            onDismiss = {
                showUpload = false
                viewModel.consumeUploadCompleted()
            },
            onUpload = { picked, type, title, description, date ->
                viewModel.upload(picked, type, title, description, date)
            },
        )
    }

    pendingDelete?.let { document ->
        ConfirmDeleteDialog(
            title = "Usunąć dokument?",
            text = "„${document.title}” zostanie trwale usunięty z serwera.",
            onConfirm = {
                pendingDelete = null
                viewModel.delete(document)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun DocumentCard(
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
private fun UploadDocumentDialog(
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
