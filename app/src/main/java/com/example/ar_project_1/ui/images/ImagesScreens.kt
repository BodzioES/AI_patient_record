package com.example.ar_project_1.ui.images

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.ImageDto
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
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.aiIsMock
import com.example.ar_project_1.ui.common.aiModeLabel
import com.example.ar_project_1.ui.common.LoadingBox
import java.util.Locale

@Composable
fun ImagesScreen(
    container: AppContainer,
    onOpenImage: (String) -> Unit,
) {
    val viewModel: ImagesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ImagesViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    var showUpload by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ImageDto?>(null) }
    val filtered = remember(state.images, state.category) {
        val category = state.category
        if (category == null) state.images else state.images.filter { it.category == category }
    }
    val series = remember(filtered) { ImagesViewModel.groupIntoSeries(filtered) }

    if (state.loading && state.images.isEmpty()) {
        LoadingBox(label = "Pobieranie zdjęć…")
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
                        "Zdjęcia (${filtered.size}/${state.images.size})",
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.category == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("Wszystkie") },
                        )
                    }
                    items(ImagesViewModel.CATEGORIES) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(ResourceCatalog.prettyEnum(category)) },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.groupSeries,
                        onClick = { viewModel.setGroupSeries(!state.groupSeries) },
                        label = { Text("Grupuj serie") },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Serie to zdjęcia powiązane z tym samym objawem.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.images.isEmpty()) "Brak zdjęć" else "Brak zdjęć w tej kategorii",
                        message = if (state.images.isEmpty()) {
                            "Dodaj zdjęcie zmiany skórnej, rany lub opakowania leku."
                        } else {
                            "Zmień filtr kategorii, aby zobaczyć pozostałe zdjęcia."
                        },
                    )
                }
            } else if (state.groupSeries) {
                series.forEach { group ->
                    item(key = "series-${group.key}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                group.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (group.images.size == 1) "1 zdjęcie" else "${group.images.size} zdjęcia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(group.images, key = { it.id }) { image ->
                        ImageCard(
                            image = image,
                            busy = state.busyId == image.id,
                            onOpen = { onOpenImage(image.id) },
                            onDelete = { pendingDelete = image },
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { image ->
                    ImageCard(
                        image = image,
                        busy = state.busyId == image.id,
                        onOpen = { onOpenImage(image.id) },
                        onDelete = { pendingDelete = image },
                    )
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
            Icon(Icons.Default.Add, contentDescription = "Dodaj zdjęcie")
        }
    }

    if (showUpload) {
        UploadImageDialog(
            uploading = state.uploading,
            uploadError = state.uploadError,
            uploadCompleted = state.uploadCompleted,
            onDismiss = {
                showUpload = false
                viewModel.consumeUploadCompleted()
            },
            onUpload = { picked, title, description, category, symptomId, capturedAt ->
                viewModel.upload(picked, title, description, category, symptomId, capturedAt)
            },
        )
    }

    pendingDelete?.let { image ->
        ConfirmDeleteDialog(
            title = "Usunąć zdjęcie?",
            text = "Zdjęcie #${image.imageNumber} zostanie trwale usunięte z serwera.",
            onConfirm = {
                pendingDelete = null
                viewModel.delete(image)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ImageCard(
    image: ImageDto,
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
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
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
                        image.uploadedAt?.let(DateInputs::toDisplay),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!image.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(image.description, style = MaterialTheme.typography.bodySmall)
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
private fun UploadImageDialog(
    uploading: Boolean,
    uploadError: String?,
    uploadCompleted: Boolean,
    onDismiss: () -> Unit,
    onUpload: (PickedFile, String, String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var pickError by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ImagesViewModel.CATEGORIES.first()) }
    var symptomId by remember { mutableStateOf("") }
    var capturedAt by remember { mutableStateOf("") }

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
        title = { Text("Nowe zdjęcie") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("image/*")) },
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (picked == null) "Wybierz zdjęcie (JPG/PNG)" else "Zmień zdjęcie")
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
                    value = category,
                    onChange = { category = it },
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł") },
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
                    value = capturedAt,
                    onValueChange = { capturedAt = it },
                    label = { Text("Data wykonania") },
                    placeholder = { Text("RRRR-MM-DD GG:MM") },
                    singleLine = true,
                    enabled = !uploading,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = symptomId,
                    onValueChange = { symptomId = it },
                    label = { Text("ID objawu (opcjonalnie)") },
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
                        pickError = "Wybierz zdjęcie do wysłania."
                    } else {
                        onUpload(file, title, description, category, symptomId, capturedAt)
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

@Composable
fun ImageDetailScreen(
    container: AppContainer,
    imageId: String,
) {
    val viewModel: ImageDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ImageDetailViewModel(container.repository, imageId) }
        },
    )
    val state = viewModel.state.value
    val aiStatus = LocalAiStatus.current
    val aiMock = aiIsMock(aiStatus)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> ErrorBanner(state.error, onRetry = viewModel::loadImage)
            state.image != null -> Card(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.Image(
                    bitmap = state.image,
                    contentDescription = "Zdjęcie medyczne",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (aiMock) "Analiza zdjęcia (MOCK)" else "Analiza zdjęcia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (aiMock) {
                        "Tryb demonstracyjny: wynik nie stanowi diagnozy ani porady medycznej."
                    } else {
                        "Model ${aiModeLabel(aiStatus)} opisze to, co widać na zdjęciu. " +
                            "To nie jest diagnoza ani porada medyczna."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = viewModel::analyze,
                    enabled = !state.analyzing && state.image != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.analyzing) "Analizowanie…" else if (aiMock) "Uruchom analizę (MOCK)" else "Uruchom analizę")
                }
                if (state.analysisError != null) {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(state.analysisError)
                }
                state.analysis?.let { analysis ->
                    Spacer(Modifier.height(16.dp))
                    if (analysis.mock) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "MOCK — to nie jest rzeczywista analiza medyczna.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(analysis.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Model: ${analysis.modelName}${analysis.modelVersion?.let { " ($it)" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        analysis.notice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.US, "%.0f kB", bytes / 1024.0)
    else -> "$bytes B"
}
