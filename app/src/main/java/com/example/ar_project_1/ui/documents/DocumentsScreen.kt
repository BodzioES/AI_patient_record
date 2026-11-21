package com.example.ar_project_1.ui.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.ui.common.ConfirmDeleteDialog
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox

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
