package com.example.ar_project_1.ui.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.ui.common.ConfirmDeleteDialog
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox

@Composable
fun ImagesScreen(
    container: AppContainer,
    onOpenImage: (String) -> Unit,
) {
    val viewModel: ImagesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ImagesViewModel(container.repository, container.imageLoader) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    var showUpload by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ImageDto?>(null) }
    val filtered = remember(state.images, state.category) {
        state.category?.let { category -> state.images.filter { it.category == category } } ?: state.images
    }
    val series = remember(filtered) { ImagesViewModel.groupIntoSeries(filtered) }

    if (state.loading && state.images.isEmpty()) {
        LoadingBox(label = "Pobieranie zdjęć…")
        return
    }

    val card: LazyListScope.(List<ImageDto>) -> Unit = { list ->
        items(list, key = { it.id }) { image ->
            ImageCard(
                image = image,
                loader = container.imageLoader,
                busy = state.busyId == image.id,
                onOpen = { onOpenImage(image.id) },
                onDelete = { pendingDelete = image },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Header(filtered.size, state.images.size, state.loading, viewModel::load) }
            item { Filters(state, viewModel) }
            state.error?.let { error -> item { ErrorBanner(error, onRetry = viewModel::load) } }
            state.message?.let { message -> item { InfoBanner(message) } }
            when {
                filtered.isEmpty() -> item {
                    EmptyState(
                        title = if (state.images.isEmpty()) "Brak zdjęć" else "Brak zdjęć w tej kategorii",
                        message = if (state.images.isEmpty()) {
                            "Dodaj zdjęcie zmiany skórnej, rany lub opakowania leku przyciskiem +."
                        } else {
                            "Zmień filtr kategorii, aby zobaczyć pozostałe zdjęcia."
                        },
                    )
                }
                state.groupSeries -> series.forEach { group ->
                    item(key = "series-${group.key}") { SeriesHeader(group) }
                    card(group.images)
                }
                else -> card(filtered)
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
            onUpload = viewModel::upload,
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
private fun Header(shown: Int, total: Int, loading: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Zdjęcia ($shown/$total)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
        }
    }
}

@Composable
private fun Filters(state: ImagesUiState, viewModel: ImagesViewModel) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = state.groupSeries,
                onClick = { viewModel.setGroupSeries(!state.groupSeries) },
                label = { Text("Grupuj serie") },
            )
        }
        item { Spacer(Modifier.width(4.dp)) }
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

@Composable
private fun SeriesHeader(group: ImageSeries) {
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

@Composable
private fun InfoBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
