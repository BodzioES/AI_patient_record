package com.example.ar_project_1.ui.charts

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ar_project_1.AppContainer
import com.example.ar_project_1.data.resource.DateInputs
import com.example.ar_project_1.ui.common.EmptyState
import com.example.ar_project_1.ui.common.ErrorBanner
import com.example.ar_project_1.ui.common.LoadingBox
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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

@Composable
private fun StatsRow(points: List<ChartPoint>, unit: String, showSecondary: Boolean) {
    val values = points.map { it.value }
    val secondary = points.mapNotNull { it.secondary }
    val last = points.last()
    val stats = listOf(
        "Ostatni" to buildString {
            append(format(last.value))
            last.secondary?.let { append(" / ").append(format(it)) }
            if (unit.isNotBlank()) append(" ").append(unit)
        },
        "Minimum" to "${format(values.minOrNull())} ${unit}".trim(),
        "Maksimum" to "${format(values.maxOrNull())} ${unit}".trim(),
        "Średnia" to "${format(values.average())} ${unit}".trim(),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
        if (showSecondary && secondary.isNotEmpty()) {
            Text(
                "Druga linia: rozkurczowe — min ${format(secondary.minOrNull())}, " +
                    "maks ${format(secondary.maxOrNull())}, średnia ${format(secondary.average())}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MeasurementChart(points: List<ChartPoint>, showSecondary: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    val allValues = buildList {
        addAll(points.map { it.value })
        if (showSecondary) addAll(points.mapNotNull { it.secondary })
    }
    if (allValues.isEmpty()) return

    val rawMin = allValues.minOrNull() ?: return
    val rawMax = allValues.maxOrNull() ?: return
    val padding = if (rawMax - rawMin < 0.0001) 1.0 else (rawMax - rawMin) * 0.1
    val minValue = rawMin - padding
    val maxValue = rawMax + padding

    val firstTime = points.first().time
    val lastTime = points.last().time
    val timeSpan = (lastTime.epochSecond - firstTime.epochSecond).coerceAtLeast(1L)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val leftPad = 34.dp.toPx()
        val rightPad = 8.dp.toPx()
        val topPad = 8.dp.toPx()
        val bottomPad = 20.dp.toPx()
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        fun xOf(time: Instant): Float {
            val ratio = (time.epochSecond - firstTime.epochSecond).toFloat() / timeSpan.toFloat()
            return leftPad + chartWidth * ratio.coerceIn(0f, 1f)
        }

        fun yOf(value: Double): Float {
            val ratio = ((value - minValue) / (maxValue - minValue)).toFloat()
            return topPad + chartHeight * (1f - ratio.coerceIn(0f, 1f))
        }

        // Siatka pozioma + etykiety osi Y.
        val gridLines = 4
        repeat(gridLines + 1) { index ->
            val value = minValue + (maxValue - minValue) * index / gridLines
            val y = yOf(value)
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(size.width - rightPad, y),
                strokeWidth = 1f,
            )
            val label = textMeasurer.measure(format(value), labelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    x = (leftPad - label.size.width - 4.dp.toPx()).coerceAtLeast(0f),
                    y = y - label.size.height / 2f,
                ),
            )
        }

        fun drawSeries(values: List<Pair<Instant, Double>>, color: Color) {
            if (values.isEmpty()) return
            if (values.size == 1) {
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(xOf(values[0].first), yOf(values[0].second)))
                return
            }
            val path = Path()
            values.forEachIndexed { index, (time, value) ->
                val x = xOf(time)
                val y = yOf(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            values.forEach { (time, value) ->
                drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(xOf(time), yOf(value)))
            }
        }

        drawSeries(points.map { it.time to it.value }, primaryColor)
        if (showSecondary) {
            val secondaryValues = points.mapNotNull { point ->
                point.secondary?.let { point.time to it }
            }
            drawSeries(secondaryValues, secondaryColor)
        }

        // Etykiety osi X: pierwsza i ostatnia data.
        val startLabel = textMeasurer.measure(shortDate(firstTime), labelStyle)
        drawText(startLabel, topLeft = Offset(leftPad, size.height - startLabel.size.height))
        val endLabel = textMeasurer.measure(shortDate(lastTime), labelStyle)
        drawText(
            endLabel,
            topLeft = Offset(
                x = (size.width - rightPad - endLabel.size.width).coerceAtLeast(leftPad),
                y = size.height - endLabel.size.height,
            ),
        )
    }
}

private fun shortDate(time: Instant): String =
    runCatching {
        time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yy"))
    }.getOrDefault("")

private fun format(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", rounded)
    }
}

private fun format(value: Double?): String = if (value == null) "—" else format(value)

private fun unitLabel(unit: String): String = if (unit.isBlank()) "bez jednostki" else unit

private fun typeLabel(type: String): String =
    ChartsViewModel.TYPES.firstOrNull { it.first == type }?.second ?: type
