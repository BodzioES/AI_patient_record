package com.example.ar_project_1.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.FormFieldInput
import com.example.ar_project_1.ui.common.LoadingBox

@Composable
fun ProfileScreen(
    container: AppContainer,
    onEdit: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileViewModel(container.repository) }
        },
    )
    LaunchedEffect(Unit) { viewModel.load() }
    val state = viewModel.state.value

    if (state.loading && state.patient == null) {
        LoadingBox(label = "Pobieranie profilu…")
        return
    }
    if (state.error != null && state.patient == null) {
        ErrorBanner(
            message = state.error,
            modifier = Modifier.padding(16.dp),
            onRetry = viewModel::load,
        )
        return
    }

    val patient = state.patient
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.error != null) ErrorBanner(state.error, onRetry = viewModel::load)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Karta pacjenta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dane widoczne tylko dla Ciebie i lokalnego asystenta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                if (patient == null) {
                    Text("Brak danych profilu.")
                } else {
                    InfoRow("Imię", patient.firstName)
                    InfoRow("Nazwisko", patient.lastName)
                    InfoRow("Data urodzenia", patient.dateOfBirth)
                    InfoRow("Płeć", patient.sex?.let(ResourceCatalog::prettyEnum))
                    InfoRow("Wzrost", patient.heightCm?.let { "$it cm" })
                    InfoRow("Waga", patient.weightKg?.let { "$it kg" })
                    InfoRow("Grupa krwi", patient.bloodType?.let(ResourceCatalog::prettyEnum))
                    if (!patient.notes.isNullOrBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Notatki", style = MaterialTheme.typography.labelMedium)
                        Text(patient.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edytuj profil")
                }
            }
        }
        Text(
            "To nie jest dokumentacja medyczna. Dane wprowadzasz na własną odpowiedzialność.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ProfileEditScreen(
    container: AppContainer,
    onSaved: () -> Unit,
) {
    val viewModel: ProfileEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ProfileEditViewModel(container.repository) }
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
        LoadingBox(label = "Pobieranie profilu…")
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
            "Edytuj profil",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (state.error != null) ErrorBanner(state.error)
        ProfileEditViewModel.PROFILE_FIELDS.forEach { field ->
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
            Text(if (state.saving) "Zapisywanie…" else "Zapisz profil")
        }
        Spacer(Modifier.height(24.dp))
    }
}
