package com.example.ar_project_1.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox
import java.time.LocalDate
import java.time.Period

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenResource: (ResourceType) -> Unit,
    onProfile: () -> Unit,
    onDocuments: () -> Unit,
    onImages: () -> Unit,
    onAssistant: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HomeViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value
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
                    title = "Asystent AI (MOCK)",
                    subtitle = "Szkielet czatu — bez rzeczywistej analizy medycznej",
                    icon = Icons.Default.SmartToy,
                    onClick = onAssistant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PatientHeader(
    patient: PatientDto?,
    onProfile: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    val name = listOfNotNull(patient?.firstName, patient?.lastName)
                        .joinToString(" ")
                        .ifBlank { "Uzupełnij profil" }
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val age = ageOf(patient?.dateOfBirth)
                    val details = listOfNotNull(
                        age,
                        patient?.bloodType?.let { "grupa ${ResourceCatalog.prettyEnum(it)}" },
                    ).joinToString(" • ")
                    Text(
                        details.ifBlank { "Dane pacjenta" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Dotknij, aby otworzyć profil i edytować dane",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfile)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PrivacyNote(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "Dane pozostają na Twoim serwerze. To projekt studencki, nie produkt medyczny.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Zamknij komunikat",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ResourceTile(
    type: ResourceType,
    count: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(iconFor(type), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(type.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (count == 1L) "1 wpis" else "$count wpisów",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun iconFor(type: ResourceType): ImageVector = when (type) {
    ResourceType.ALLERGIES -> Icons.Default.Warning
    ResourceType.CONDITIONS -> Icons.Default.MedicalInformation
    ResourceType.MEDICATIONS -> Icons.Default.Medication
    ResourceType.SYMPTOMS -> Icons.Default.Sick
    ResourceType.MEASUREMENTS -> Icons.Default.MonitorHeart
    ResourceType.DOCTORS -> Icons.Default.Person
    ResourceType.APPOINTMENTS -> Icons.Default.Event
}

private fun ageOf(dateOfBirth: String?): String? {
    if (dateOfBirth.isNullOrBlank()) return null
    return runCatching {
        val birth = LocalDate.parse(dateOfBirth)
        val years = Period.between(birth, LocalDate.now()).years
        "$years lat"
    }.getOrNull()
}
