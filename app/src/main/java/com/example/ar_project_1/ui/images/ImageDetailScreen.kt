package com.example.ar_project_1.ui.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.data.resource.ResourceCatalog
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LocalAiStatus
import com.example.ar_project_1.ui.common.RemoteImage
import com.example.ar_project_1.ui.common.RemoteImageLoader
import com.example.ar_project_1.ui.common.aiIsMock
import com.example.ar_project_1.ui.common.aiModeLabel

@Composable
fun ImageDetailScreen(
    container: AppContainer,
    imageId: String,
) {
    val viewModel: ImageDetailViewModel = viewModel(
        key = "image-$imageId",
        factory = viewModelFactory {
            initializer { ImageDetailViewModel(container.repository, imageId) }
        },
    )
    val state = viewModel.state.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            RemoteImage(
                imageId = imageId,
                loader = container.imageLoader,
                maxSide = RemoteImageLoader.FULL,
                contentScale = ContentScale.Fit,
                contentDescription = "Zdjęcie medyczne",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )
        }
        state.info?.let { info ->
            Column {
                Text(
                    info.title ?: "Zdjęcie #${info.imageNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    listOfNotNull(
                        "#${info.imageNumber}",
                        ResourceCatalog.prettyEnum(info.category),
                        (info.capturedAt ?: info.uploadedAt)?.let(DateInputs::toDisplay),
                        formatSize(info.fileSizeBytes),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!info.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(info.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        AnalysisCard(state, viewModel::analyze)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AnalysisCard(state: ImageDetailUiState, onAnalyze: () -> Unit) {
    val aiStatus = LocalAiStatus.current
    val aiMock = aiIsMock(aiStatus)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (aiMock) "Analiza zdjęcia (MOCK)" else "Analiza zdjęcia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (aiMock) {
                    "Tryb demonstracyjny: wynik nie stanowi diagnozy ani porady medycznej."
                } else {
                    "Model ${aiModeLabel(aiStatus)} opisze to, co widać na zdjęciu. " +
                        "To nie jest diagnoza ani porada medyczna."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAnalyze,
                enabled = !state.analyzing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        state.analyzing -> "Analizowanie…"
                        aiMock -> "Uruchom analizę (MOCK)"
                        else -> "Uruchom analizę"
                    },
                )
            }
            state.analysisError?.let {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(it)
            }
            state.analysis?.let { analysis ->
                Spacer(Modifier.height(16.dp))
                if (analysis.mock) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "MOCK — to nie jest rzeczywista analiza medyczna.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(analysis.text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Model: ${analysis.modelName}${analysis.modelVersion?.let { " ($it)" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(analysis.notice, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
