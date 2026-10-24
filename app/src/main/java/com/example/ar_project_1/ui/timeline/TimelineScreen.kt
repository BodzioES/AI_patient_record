package com.example.ar_project_1.ui.timeline

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.TimelineEventDto
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox
import com.example.ar_project_1.ui.common.StatusChip

/**
 * Wspólna chronologia: wizyty, pomiary, objawy, leki, dokumenty i zdjęcia
 * w jednej osi czasu, z filtrem rodzaju zdarzenia.
 */
@Composable
fun TimelineScreen(
    container: AppContainer,
    onOpenEvent: (TimelineEventDto) -> Unit,
) {
    val viewModel: TimelineViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TimelineViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value

    if (state.loading && state.events.isEmpty()) {
        LoadingBox(label = "Pobieranie chronologii…")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimelineViewModel.FILTERS) { (type, label) ->
                    FilterChip(
                        selected = state.filter == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (state.error != null) {
            item { ErrorBanner(state.error, onRetry = viewModel::load) }
        }

        if (state.events.isEmpty()) {
            item {
                EmptyState(
                    title = "Chronologia jest pusta",
                    message = "Dodaj wizytę, pomiar lub inny wpis — pojawią się tutaj w kolejności dat.",
                )
            }
        } else {
            items(
                items = state.events,
                key = { "${it.type}-${it.id}" },
            ) { event ->
                EventCard(event = event, onClick = { onOpenEvent(event) })
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
                            Text("Pokaż więcej (${state.events.size}/${state.total})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: TimelineEventDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconFor(event.type),
                        contentDescription = typeLabel(event.type),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!event.subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        event.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(
                        typeLabel(event.type),
                        event.occurredAt?.let(DateInputs::toDisplay)?.ifBlank { null },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (!event.status.isNullOrBlank()) {
                StatusChip(event.status)
            }
        }
    }
}

fun iconFor(type: String): ImageVector = when (type) {
    "APPOINTMENT" -> Icons.Default.Event
    "MEASUREMENT" -> Icons.Default.MonitorHeart
    "SYMPTOM" -> Icons.Default.Sick
    "MEDICATION" -> Icons.Default.Medication
    "DOCUMENT" -> Icons.Default.Description
    "IMAGE" -> Icons.Default.Image
    else -> Icons.Default.Description
}

fun typeLabel(type: String): String = when (type) {
    "APPOINTMENT" -> "Wizyta"
    "MEASUREMENT" -> "Pomiar"
    "SYMPTOM" -> "Objaw"
    "MEDICATION" -> "Lek"
    "DOCUMENT" -> "Dokument"
    "IMAGE" -> "Zdjęcie"
    else -> ResourceCatalog.prettyEnum(type)
}
