package com.example.ar_project_1.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.aiIsMock
import com.example.ar_project_1.ui.common.aiModeLabel

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenResource: (ResourceType) -> Unit,
    onProfile: () -> Unit,
    onDocuments: () -> Unit,
    onImages: () -> Unit,
    onAssistant: () -> Unit,
    onTimeline: () -> Unit,
    onCharts: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
    val aiStatus = LocalAiStatus.current
    val aiMock = aiIsMock(aiStatus)
    var privacyDismissed by rememberSaveable { mutableStateOf(false) }

    if (state.loading && state.patient == null) {
        LoadingBox(label = "Pobieranie karty pacjenta…")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!privacyDismissed) {
            item {
                PrivacyNote(onDismiss = { privacyDismissed = true })
            }
        }
        if (state.error != null) {
            item { ErrorBanner(state.error, onRetry = viewModel::load) }
        }
        item {
            PatientHeader(
                patient = state.patient,
                onProfile = onProfile,
                onRefresh = viewModel::load,
                refreshing = state.loading,
            )
        }
        item {
            Text(
                "Moje dane",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(ResourceType.entries.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { type ->
                    ResourceTile(
                        type = type,
                        count = state.counts[type] ?: 0,
                        onClick = { onOpenResource(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (state.timeline.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Ostatnia aktywność",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Zobacz wszystkie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onTimeline)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                    )
                }
            }
            items(state.timeline) { event ->
                TimelinePreviewRow(event = event, onClick = onTimeline)
            }
        } else {
            item {
                OutlinedButton(
                    onClick = onTimeline,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Otwórz chronologię")
                }
            }
        }
        item {
            Text(
                "Pliki i asystent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureTile(
                    title = "Dokumenty",
                    subtitle = "PDF i zdjęcia dokumentów",
                    icon = Icons.Default.Description,
                    onClick = onDocuments,
                    modifier = Modifier.fillMaxWidth(),
                )
                FeatureTile(
                    title = "Zdjęcia",
                    subtitle = "Zdjęcia zmian i ran",
                    icon = Icons.Default.Image,
                    onClick = onImages,
                    modifier = Modifier.fillMaxWidth(),
                )
                FeatureTile(
                    title = "Wykresy pomiarów",
                    subtitle = "Trend ciśnienia, tętna, wagi i innych",
                    icon = Icons.Default.MonitorHeart,
                    onClick = onCharts,
                    modifier = Modifier.fillMaxWidth(),
                )
                FeatureTile(
                    title = if (aiMock) "Asystent AI (MOCK)" else "Asystent AI",
                    subtitle = if (aiMock) {
                        "Tryb demonstracyjny — bez rzeczywistej analizy medycznej"
                    } else {
                        "Model lokalny: ${aiModeLabel(aiStatus)}"
                    },
                    icon = Icons.Default.SmartToy,
                    onClick = onAssistant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
