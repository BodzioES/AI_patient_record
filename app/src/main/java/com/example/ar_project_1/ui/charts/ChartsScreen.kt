package com.example.ar_project_1.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox

/**
 * Wykresy pomiarów: trend w czasie, statystyki i druga linia dla ciśnienia
 * (skurczowe/rozkurczowe). Dane pobierane są z backendu, maks. 200 punktów.
 */
@Composable
fun ChartsScreen(container: AppContainer) {
    val viewModel: ChartsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ChartsViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    val points = remember(state.points) { state.points.sortedBy { it.time } }

    if (state.loading && points.isEmpty()) {
        LoadingBox(label = "Pobieranie pomiarów…")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChartsViewModel.TYPES) { (type, label) ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(label) },
                    )
                }
            }
        }

        if (state.error != null) {
            item { ErrorBanner(state.error, onRetry = viewModel::load) }
        }

        if (points.isEmpty()) {
            item {
                EmptyState(
                    title = "Brak pomiarów",
                    message = "Dodaj pomiar w sekcji „Pomiary”, aby zobaczyć wykres trendu.",
                )
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            typeLabel(state.type),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Punkty: ${points.size} • ${unitLabel(state.unit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        MeasurementChart(
                            points = points,
                            showSecondary = state.type == "BLOOD_PRESSURE",
                        )
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Statystyki",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        StatsRow(points = points, unit = state.unit, showSecondary = state.type == "BLOOD_PRESSURE")
                    }
                }
            }

            item {
                Text(
                    "Ostatnie pomiary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(points.takeLast(10).reversed()) { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        DateInputs.toDisplay(point.time.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        buildString {
                            append(format(point.value))
                            point.secondary?.let { append(" / ").append(format(it)) }
                            if (state.unit.isNotBlank()) append(" ").append(state.unit)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
