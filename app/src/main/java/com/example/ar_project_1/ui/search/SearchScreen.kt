package com.example.ar_project_1.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.SearchHitDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import kotlinx.coroutines.delay

/**
 * Wyszukiwanie po całej karcie pacjenta: alergie, schorzenia, leki, objawy,
 * pomiary, lekarze, wizyty, dokumenty i zdjęcia.
 */
@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenHit: (SearchHitDto) -> Unit,
) {
    val viewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(container.repository) }
        },
    )
    val state = viewModel.state.value

    LaunchedEffect(state.query) {
        if (state.query.isBlank()) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        viewModel.search()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Szukaj w karcie pacjenta") },
            placeholder = { Text("np. penicylina, ciśnienie, Kowalski") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clear) {
                        Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
        )

        Spacer(Modifier.height(8.dp))

        if (state.error != null) {
            ErrorBanner(state.error, onRetry = viewModel::search)
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }

            state.query.trim().length < SearchViewModel.MIN_QUERY_LENGTH -> Hint()

            state.searched && state.hits.isEmpty() -> EmptyState(
                title = "Brak wyników",
                message = "Nie znaleziono wpisów zawierających „${state.query.trim()}”.",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "Znaleziono: ${state.total}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.hits, key = { "${it.type}-${it.id}" }) { hit ->
                    HitCard(hit = hit, onClick = { onOpenHit(hit) })
                }
            }
        }
    }
}

@Composable
private fun Hint() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Wpisz co najmniej ${SearchViewModel.MIN_QUERY_LENGTH} znaki. Szukamy w nazwach, opisach i notatkach.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun HitCard(hit: SearchHitDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconFor(hit.type),
                        contentDescription = typeLabel(hit.type),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    hit.title.ifBlank { typeLabel(hit.type) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!hit.subtitle.isNullOrBlank()) {
                    Text(
                        hit.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    listOfNotNull(
                        typeLabel(hit.type),
                        hit.occurredAt?.let(DateInputs::toDisplay)?.ifBlank { null },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

fun typeLabel(type: String): String = when (type) {
    "ALLERGY" -> "Alergia"
    "CONDITION" -> "Schorzenie"
    "MEDICATION" -> "Lek"
    "SYMPTOM" -> "Objaw"
    "MEASUREMENT" -> "Pomiar"
    "DOCTOR" -> "Lekarz"
    "APPOINTMENT" -> "Wizyta"
    "DOCUMENT" -> "Dokument"
    "IMAGE" -> "Zdjęcie"
    else -> type
}

private fun iconFor(type: String): ImageVector = when (type) {
    "ALLERGY" -> Icons.Default.Warning
    "CONDITION" -> Icons.Default.MedicalInformation
    "MEDICATION" -> Icons.Default.Medication
    "SYMPTOM" -> Icons.Default.Sick
    "MEASUREMENT" -> Icons.Default.MonitorHeart
    "DOCTOR" -> Icons.Default.Person
    "APPOINTMENT" -> Icons.Default.Event
    "DOCUMENT" -> Icons.Default.Description
    "IMAGE" -> Icons.Default.Image
    else -> Icons.Default.Search
}

private const val DEBOUNCE_MS = 350L
