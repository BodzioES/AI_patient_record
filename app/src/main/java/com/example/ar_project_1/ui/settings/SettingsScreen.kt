package com.example.ar_project_1.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.export.ExportService
import com.example.ar_project_1.ui.common.LocalAiStatus
import kotlinx.coroutines.launch

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
        AccountSection(session.displayName, session.email, loggingOut) { showLogoutDialog = true }
        ExportSection(exportState, ::export)
        TextSizeSection(textScale, onTextScaleChange)
        CacheSection {
            scope.launch {
                container.fileSaver.clearCache()
                container.imagePreparer.clear()
                message = "Pamięć podręczna została wyczyszczona."
            }
        }
        AboutSection(aiStatus)
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
