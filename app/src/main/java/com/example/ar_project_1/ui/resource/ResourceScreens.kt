package com.example.ar_project_1.ui.resource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.common.ConfirmDeleteDialog
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.FormFieldInput
import com.example.ar_project_1.ui.common.LoadingBox
import com.example.ar_project_1.ui.common.StatusChip

@Composable
fun ResourceListScreen(
    container: AppContainer,
    type: ResourceType,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val viewModel: ResourceListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ResourceListViewModel(container.repository, type) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    var pendingDelete by remember { mutableStateOf<ResourceRow?>(null) }
    var sortExpanded by remember { mutableStateOf(false) }
    val sorts = ResourceListViewModel.sortsFor(type)
    val currentSort = sorts.firstOrNull { it.value == state.sort } ?: sorts.first()
    val visibleRows = remember(state.rows, state.filter) {
        val query = state.filter.trim().lowercase()
        if (query.isEmpty()) {
            state.rows
        } else {
            state.rows.filter { row ->
                row.summary.title.lowercase().contains(query) ||
                    row.summary.subtitle.lowercase().contains(query) ||
                    row.summary.status?.lowercase()?.contains(query) == true
            }
        }
    }

    if (state.loading && state.rows.isEmpty()) {
        LoadingBox(label = "Pobieranie: ${type.title.lowercase()}…")
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
                        "${type.title} (${state.rows.size}/${state.total})",
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
                OutlinedTextField(
                    value = state.filter,
                    onValueChange = viewModel::setFilter,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Filtruj pobrane wpisy") },
                    placeholder = { Text("np. penicylina, wizyta, aktywny") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                )
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
                            Text(currentSort.label)
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                        ) {
                            sorts.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sortExpanded = false
                                        viewModel.setSort(option.value)
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
            if (visibleRows.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.rows.isEmpty()) "Brak wpisów" else "Brak dopasowań",
                        message = if (state.rows.isEmpty()) {
                            "Dodaj pierwszy wpis w sekcji „${type.title}”."
                        } else {
                            "Żaden z pobranych wpisów nie zawiera „${state.filter.trim()}”."
                        },
                    )
                }
            } else {
                items(visibleRows, key = { it.id }) { row ->
                    ResourceCard(
                        row = row,
                        deleting = state.deletingId == row.id,
                        onEdit = { onEdit(row.id) },
                        onDelete = { pendingDelete = row },
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
                            CircularProgressIndicator(
                                modifier = Modifier.width(24.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            OutlinedButton(onClick = viewModel::loadMore) {
                                Text("Pokaż więcej (${state.rows.size}/${state.total})")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Dodaj")
        }
    }

    pendingDelete?.let { row ->
        ConfirmDeleteDialog(
            title = "Usunąć wpis?",
            text = "„${row.summary.title}” zostanie trwale usunięty.",
            onConfirm = {
                pendingDelete = null
                viewModel.delete(row.id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ResourceCard(
    row: ResourceRow,
    deleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    row.summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    row.summary.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (row.summary.status != null) {
                    Spacer(Modifier.height(6.dp))
                    StatusChip(row.summary.status)
                }
            }
            if (deleting) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń")
                }
            }
        }
    }
}

@Composable
fun ResourceFormScreen(
    container: AppContainer,
    type: ResourceType,
    id: String?,
    onSaved: () -> Unit,
) {
    val viewModel: ResourceFormViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ResourceFormViewModel(container.repository, type, id) }
        },
    )
    val state = viewModel.state.value
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onSaved()
        }
    }

    if (state.loading) {
        LoadingBox(label = "Pobieranie wpisu…")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (id.isNullOrBlank()) "Nowy wpis: ${type.title}" else "Edytuj: ${type.title}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (state.error != null) ErrorBanner(state.error)
        com.example.ar_project_1.data.resource.ResourceCatalog.fields(type).forEach { field ->
            FormFieldInput(
                field = field,
                value = state.values[field.key].orEmpty(),
                onChange = { viewModel.update(field.key, it) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::save,
            enabled = !state.saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(if (state.saving) "Zapisywanie…" else "Zapisz")
        }
        Spacer(Modifier.height(24.dp))
    }
}
