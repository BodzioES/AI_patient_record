package com.example.ar_project_1.ui.resource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.ResourceType
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.FormFieldInput
import com.example.ar_project_1.ui.common.LoadingBox

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
