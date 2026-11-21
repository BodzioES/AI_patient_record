package com.example.ar_project_1.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ar_project_1.BuildConfig
import com.example.ar_project_1.data.model.AiStatusDto
import com.example.ar_project_1.ui.common.aiModeLabel
import kotlin.math.abs

private val TEXT_SCALES = listOf(
    0.9f to "Mniejszy",
    1.0f to "Domyślny",
    1.15f to "Większy",
    1.3f to "Duży",
    1.4f to "Największy",
)

@Composable
private fun SettingsCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun AccountSection(name: String?, email: String?, loggingOut: Boolean, onLogout: () -> Unit) {
    SettingsCard(Icons.Default.Person, "Konto") {
        Text(name ?: "Pacjent", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Hint(email ?: "—")
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onLogout, enabled = !loggingOut, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (loggingOut) "Wylogowywanie…" else "Wyloguj się")
        }
    }
}

@Composable
internal fun ExportSection(state: ExportUiState, onExport: (ExportFormat) -> Unit) {
    SettingsCard(Icons.Default.FileDownload, "Eksport danych") {
        Hint("Zapisz kopię karty pacjenta jako plik CSV lub PDF. Plik powstaje na telefonie — nic nie jest wysyłane na zewnątrz.")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExportFormat.entries.forEach { format ->
                OutlinedButton(
                    onClick = { onExport(format) },
                    enabled = !state.preparing,
                    modifier = Modifier.weight(1f),
                ) { Text(format.name) }
            }
        }
        if (state.preparing) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Hint("Zbieranie danych z serwera…")
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun TextSizeSection(textScale: Float, onChange: (Float) -> Unit) {
    SettingsCard(Icons.Default.TextFields, "Dostępność") {
        Hint("Rozmiar tekstu w całej aplikacji. Ustawienie zapisuje się na tym urządzeniu.")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TEXT_SCALES) { (scale, label) ->
                FilterChip(
                    selected = abs(textScale - scale) < 0.01f,
                    onClick = { onChange(scale) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
internal fun CacheSection(onClear: () -> Unit) {
    SettingsCard(Icons.Default.Storage, "Pamięć podręczna") {
        Hint("Pobrane pliki i przygotowane do wysyłki zdjęcia są przechowywane tymczasowo w pamięci podręcznej aplikacji.")
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
            Text("Wyczyść pamięć podręczną")
        }
    }
}

@Composable
internal fun AboutSection(aiStatus: AiStatusDto?) {
    SettingsCard(Icons.Default.Info, "O aplikacji") {
        Text("Karta Pacjenta ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        Hint("Projekt studencki. Nie jest wyrobem medycznym i nie służy do diagnozowania ani leczenia.")
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Hint(
            if (aiStatus?.mock != false) {
                "Asystent AI działa w trybie MOCK: odpowiedzi są demonstracyjne i nie pochodzą z modelu językowego."
            } else {
                "Asystent AI korzysta z lokalnego modelu ${aiModeLabel(aiStatus)} (LM Studio). " +
                    "Odpowiedzi nie są poradą medyczną."
            },
        )
        Spacer(Modifier.height(8.dp))
        Hint("Adres serwera jest wykrywany i ustawiany automatycznie — nie trzeba go wpisywać.")
    }
}
