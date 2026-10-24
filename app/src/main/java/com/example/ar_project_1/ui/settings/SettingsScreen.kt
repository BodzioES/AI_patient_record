package com.example.ar_project_1.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.BuildConfig
import com.example.ar_project_1.data.export.ExportService
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.aiModeLabel
import kotlinx.coroutines.launch

private val TEXT_SCALES = listOf(
    0.9f to "Mniejszy",
    1.0f to "Domyślny",
    1.15f to "Większy",
    1.3f to "Duży",
    1.4f to "Największy",
)

@Composable
fun SettingsScreen(
    container: AppContainer,
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = container.sessionManager.current()
    val aiStatus = LocalAiStatus.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var loggingOut by remember { mutableStateOf(false) }

    val exportViewModel: ExportViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ExportViewModel(container.repository, ExportService()) }
        },
    )
    val exportState = exportViewModel.state.value
    var preparedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var preparedFormat by remember { mutableStateOf<ExportFormat?>(null) }

    fun saveTo(uri: Uri?) {
        val bytes = preparedBytes
        val format = preparedFormat
        if (uri == null || bytes == null || format == null) {
            preparedBytes = null
            preparedFormat = null
            return
        }
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Nie można otworzyć pliku do zapisu.")
        }.onSuccess {
            exportViewModel.reportSaved(format.fileName)
        }.onFailure {
            exportViewModel.reportError(it.message ?: "Nie udało się zapisać pliku.")
        }
        preparedBytes = null
        preparedFormat = null
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> saveTo(uri) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> saveTo(uri) }

    fun export(format: ExportFormat) {
        if (exportState.preparing) return
        exportViewModel.clearMessages()
        scope.launch {
            val bytes = exportViewModel.build(format) ?: return@launch
            preparedBytes = bytes
            preparedFormat = format
            when (format) {
                ExportFormat.CSV -> csvLauncher.launch(format.fileName)
                ExportFormat.PDF -> pdfLauncher.launch(format.fileName)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        message?.let {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Konto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    session.displayName ?: "Pacjent",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    session.email ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    enabled = !loggingOut,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loggingOut) "Wylogowywanie…" else "Wyloguj się")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Eksport danych", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Zapisz kopię karty pacjenta jako plik CSV lub PDF. Plik powstaje na telefonie — " +
                        "nic nie jest wysyłane na zewnątrz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { export(ExportFormat.CSV) },
                        enabled = !exportState.preparing,
                        modifier = Modifier.weight(1f),
                    ) { Text("CSV") }
                    OutlinedButton(
                        onClick = { export(ExportFormat.PDF) },
                        enabled = !exportState.preparing,
                        modifier = Modifier.weight(1f),
                    ) { Text("PDF") }
                }
                if (exportState.preparing) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Zbieranie danych z serwera…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                exportState.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                exportState.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TextFields, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dostępność", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rozmiar tekstu w całej aplikacji. Ustawienie zapisuje się na tym urządzeniu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TEXT_SCALES) { (scale, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(textScale - scale) < 0.01f,
                            onClick = { onTextScaleChange(scale) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pamięć podręczna", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pobrane pliki są przechowywane tymczasowo w pamięci podręcznej aplikacji.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            container.fileSaver.clearCache()
                            message = "Pamięć podręczna została wyczyszczona."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Wyczyść pobrane pliki")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("O aplikacji", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Text("Karta Pacjenta ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Projekt studencki. Nie jest wyrobem medycznym i nie służy do diagnozowania ani leczenia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    if (aiStatus?.mock != false) {
                        "Asystent AI działa w trybie MOCK: odpowiedzi są demonstracyjne i nie pochodzą z modelu językowego."
                    } else {
                        "Asystent AI korzysta z lokalnego modelu ${aiModeLabel(aiStatus)} (LM Studio). " +
                            "Odpowiedzi nie są poradą medyczną."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Adres serwera jest wykrywany i ustawiany automatycznie — nie trzeba go wpisywać.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "Dane są przechowywane wyłącznie na Twoim serwerze. " +
                "Tokeny sesji są szyfrowane kluczem z Android Keystore.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            text = { Text("Zapisane lokalnie tokeny zostaną usunięte. Dane medyczne pozostaną na serwerze.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        loggingOut = true
                        scope.launch {
                            container.repository.logout()
                            loggingOut = false
                            onLoggedOut()
                        }
                    },
                ) { Text("Wyloguj") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Anuluj") }
            },
        )
    }
}
